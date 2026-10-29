"""Isolated lifecycle checks against Android API fakes; does not replace device QA."""
from pathlib import Path
import os
import subprocess
import tempfile

STUBS = {
'android/content/Context.java': '''package android.content;
public class Context { public static final String AUDIO_SERVICE="audio";
 public final android.media.AudioManager manager=new android.media.AudioManager();
 public Object getSystemService(String key){return manager;} }''',
'android/content/SharedPreferences.java': '''package android.content;
public interface SharedPreferences {boolean getBoolean(String k,boolean d);int getInt(String k,int d);}''',
'android/media/AudioManager.java': '''package android.media;
public class AudioManager {
 public interface OnAudioFocusChangeListener {void onAudioFocusChange(int v);}
 public static final int STREAM_MUSIC=3,AUDIOFOCUS_GAIN=1,AUDIOFOCUS_REQUEST_GRANTED=1,
 AUDIOFOCUS_LOSS=-1,AUDIOFOCUS_LOSS_TRANSIENT=-2,AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK=-3;
 public boolean grant=true; public int requests,abandons;
 public int requestAudioFocus(OnAudioFocusChangeListener l,int s,int g){requests++;return grant?1:0;}
 public void abandonAudioFocus(OnAudioFocusChangeListener l){abandons++;}
}''',
'android/media/MediaPlayer.java': '''package android.media;
public class MediaPlayer {
 public static final java.util.List<MediaPlayer> all=new java.util.ArrayList<>();
 public boolean playing,looping,released; public float volume;
 public static MediaPlayer create(android.content.Context c,int r){MediaPlayer p=new MediaPlayer();all.add(p);return p;}
 public void setLooping(boolean b){looping=b;} public void setVolume(float a,float b){volume=a;}
 public void start(){if(released)throw new AssertionError("start after release");playing=true;}
 public void pause(){playing=false;} public boolean isPlaying(){return playing;}
 public void release(){playing=false;released=true;}
}''',
'com/flappybird/recreation/R.java': '''package com.flappybird.recreation;
class R {static class raw {static int nengue_chiptune=1,police_8bit=2,night_shift=3;}}''',
'com/flappybird/recreation/AudioLifecycleTest.java': '''package com.flappybird.recreation;
import android.media.*;import android.content.*;
class AudioLifecycleTest {
 static class Prefs implements SharedPreferences {
  boolean music=true,siren=true;int mv=35,sv=18;
  public boolean getBoolean(String k,boolean d){return k.equals(RetroAudio.MUSIC)?music:siren;}
  public int getInt(String k,int d){return k.equals(RetroAudio.MUSIC_VOLUME)?mv:sv;}
 }
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static boolean near(float a,float b){return Math.abs(a-b)<.0001;}
 public static void main(String[] args){
  Context c=new Context();Prefs prefs=new Prefs();RetroAudio a=new RetroAudio(c);
  MediaPlayer m=MediaPlayer.all.get(0),s=MediaPlayer.all.get(1);
  a.resume(prefs);check(m.playing&&s.playing&&m.looping&&s.looping,"foreground loops");
  check(near(m.volume,.35f)&&near(s.volume,.18f),"initial volumes");
  a.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);check(!m.playing&&!s.playing,"transient pause");
  a.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);check(m.playing&&s.playing,"focus resume");
  a.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);check(near(m.volume,.07f),"duck volume");
  a.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);check(near(m.volume,.35f),"restore volume");
  a.pause();a.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);check(!m.playing&&!s.playing,"never start in background");
  prefs.music=false;a.resume(prefs);check(!m.playing&&s.playing,"independent siren");a.pause();
  prefs.siren=false;int requests=c.manager.requests;a.resume(prefs);
  check(c.manager.requests==requests&&!m.playing&&!s.playing,"disabled audio does not request focus");a.pause();
  prefs.music=true;c.manager.grant=false;a.resume(prefs);check(!m.playing&&!s.playing,"denied focus stays silent");a.pause();
  c.manager.grant=true;prefs.mv=1000;a.resume(prefs);check(near(m.volume,1f),"clamp volume");
  a.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);a.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
  check(!m.playing&&!s.playing,"permanent loss stays silent");
  a.resume(prefs);MediaPlayer n=MediaPlayer.all.get(2);
  a.setNightShift(true);check(n.playing&&!m.playing&&n.looping,"night shift replaces normal music");
  a.pause();check(!n.playing,"night shift pauses in background");
  a.setNightShift(false);check(!m.playing&&!n.playing,"changing phase cannot start background audio");
  a.resume(prefs);check(m.playing&&!n.playing,"restart restores normal music");
  a.release();check(m.released&&s.released&&n.released,"release all players");
  System.out.println("Audio lifecycle: all checks passed (isolated API fakes)");
 }
}''',
}

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='nengue-audio-test-') as folder:
        tmp=Path(folder)
        for name,source in STUBS.items():
            p=tmp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8')
        sources=[str(p) for p in tmp.rglob('*.java')]
        sources.append(str(root/'app/src/main/java/com/flappybird/recreation/RetroAudio.java'))
        subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(tmp),*sources],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.AudioLifecycleTest'],check=True)

if __name__=='__main__':main()

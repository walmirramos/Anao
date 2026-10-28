"""Lifecycle checks for the actual falling-burst controller, using a SoundPool fake."""
from pathlib import Path
import os, subprocess, tempfile

STUB = '''package android.media;
public class SoundPool {
 public int plays,stops,loop;public float volume;
 public int play(int sample,float l,float r,int priority,int repeat,float rate){
  plays++;loop=repeat;volume=l;return plays;
 }
 public void stop(int stream){stops++;}
}'''
TEST = '''package com.flappybird.recreation;
import android.media.SoundPool;
class FallingGunfireTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args){
  SoundPool p=new SoundPool();FallingGunfire shots=new FallingGunfire(p,7);
  shots.resume(true);check(p.plays==0,"no shots before a wall collision");
  shots.begin(true);check(p.plays==1&&p.loop==-1&&p.volume<.4f,"quiet falling loop");
  shots.begin(true);check(p.plays==1,"no duplicate streams");
  shots.pause();check(p.stops==1,"pause stops the loop");
  shots.resume(true);check(p.plays==2,"resume unfinished fall");
  shots.land();check(p.stops==2,"touchdown stops shots");
  shots.resume(true);check(p.plays==2,"no restart after landing");
  shots.begin(false);check(p.plays==2,"muted collision remains silent");
  shots.resume(true);check(p.plays==3,"unmute during fall");
  shots.resume(false);check(p.stops==3,"disable effects stops loop");
  shots.land();shots.resume(true);check(p.plays==3,"reset clears pending fall");
  System.out.println("Falling gunfire: 11 lifecycle checks passed (isolated SoundPool fake)");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-gunfire-') as folder:
        tmp=Path(folder);stub=tmp/'SoundPool.java';stub.write_text(STUB,encoding='utf-8')
        test=tmp/'FallingGunfireTest.java';test.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(stub),str(test),
            str(root/'app/src/main/java/com/flappybird/recreation/FallingGunfire.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.FallingGunfireTest'],check=True)

if __name__=='__main__':main()

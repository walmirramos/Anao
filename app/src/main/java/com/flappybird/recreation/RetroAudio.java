package com.flappybird.recreation;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;

/** Foreground-only, focus-aware playback of the original synthesized arcade loops. */
final class RetroAudio implements AudioManager.OnAudioFocusChangeListener {
    static final String MUSIC = "retro_music", SIREN = "retro_siren";
    static final String MUSIC_VOLUME = "retro_music_volume", SIREN_VOLUME = "retro_siren_volume";
    private final AudioManager manager;
    private MediaPlayer music, siren, night;
    private boolean nightShift;
    private boolean foreground, musicEnabled, sirenEnabled, focused;
    private float musicVolume, sirenVolume;

    RetroAudio(Context context) {
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        music = MediaPlayer.create(context, R.raw.nengue_chiptune);
        siren = MediaPlayer.create(context, R.raw.police_8bit);
        night=MediaPlayer.create(context,R.raw.night_shift);
        if(night!=null)night.setLooping(true);
        if (music != null) music.setLooping(true);
        if (siren != null) siren.setLooping(true);
    }

    void resume(SharedPreferences prefs) {
        foreground = true;
        musicEnabled = prefs.getBoolean(MUSIC, true);
        sirenEnabled = prefs.getBoolean(SIREN, true);
        musicVolume = Math.max(0, Math.min(100, prefs.getInt(MUSIC_VOLUME, 22))) / 100f;
        sirenVolume = Math.max(0, Math.min(100, prefs.getInt(SIREN_VOLUME, 48))) / 100f;
        if (!musicEnabled && !sirenEnabled) return;
        focused = manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (focused) play();
    }

    void setNightShift(boolean enabled) {
        if(nightShift==enabled)return;
        stopPlayback();nightShift=enabled;
        if(foreground && focused)play();
    }
    private void play() {
        volume(1f);
        if(musicEnabled) {
            MediaPlayer selected=nightShift ? night : music;
            if(selected!=null)selected.start();
        }
        if (sirenEnabled && siren != null) siren.start();
    }

    private void volume(float multiplier) {
        if (music != null) music.setVolume(musicVolume * multiplier, musicVolume * multiplier);
        if(night!=null)night.setVolume(musicVolume*multiplier,musicVolume*multiplier);
        if (siren != null) siren.setVolume(sirenVolume * multiplier, sirenVolume * multiplier);
    }

    private void stopPlayback() {
        if (music != null && music.isPlaying()) music.pause();
        if (siren != null && siren.isPlaying()) siren.pause();
        if (night != null && night.isPlaying()) night.pause();
    }

    void pause() {
        foreground = false;
        stopPlayback();
        manager.abandonAudioFocus(this);
        focused = false;
    }

    @Override public void onAudioFocusChange(int change) {
        if (change == AudioManager.AUDIOFOCUS_GAIN) {
            focused = true;
            if (foreground) play();
        } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            volume(.2f);
        } else {
            focused = false;
            stopPlayback();
            if (change == AudioManager.AUDIOFOCUS_LOSS) foreground = false;
        }
    }

    void release() {
        pause();
        if (music != null) { music.release(); music = null; }
        if (siren != null) { siren.release(); siren = null; }
        if(night!=null){night.release();night=null;}
    }
}

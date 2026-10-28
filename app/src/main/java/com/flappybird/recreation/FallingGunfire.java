package com.flappybird.recreation;

import android.media.SoundPool;

/** A quiet arcade burst only during the fall after a wall collision. */
final class FallingGunfire {
    private final SoundPool pool;
    private final int sample;
    private int stream;
    private boolean falling;

    FallingGunfire(SoundPool pool, int sample) { this.pool=pool; this.sample=sample; }

    void begin(boolean enabled) {
        falling=true;
        resume(enabled);
    }

    void resume(boolean enabled) {
        if (!enabled) { pause(); return; }
        if (falling && stream==0) stream=pool.play(sample,.28f,.28f,2,-1,1f);
    }

    void pause() {
        if (stream!=0) { pool.stop(stream); stream=0; }
    }

    void land() { falling=false; pause(); }
}

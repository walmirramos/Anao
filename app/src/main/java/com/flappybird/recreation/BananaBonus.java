package com.flappybird.recreation;

/** Gameplay state only; the timer is advanced by foreground gameplay time. */
final class BananaBonus {
    private int nextScore=20;
    private boolean visible;
    private float remaining;

    boolean shouldSpawn(int score) { return score>=nextScore && !visible && !active(); }
    void spawned(int score, int gap) { visible=true;nextScore=score+Math.max(10,gap); }
    boolean collect() {
        if(!visible) return false;
        visible=false;remaining=10f;return true;
    }
    void miss() { visible=false; }
    void tick(float seconds) { remaining=Math.max(0,remaining-Math.max(0,seconds)); }
    boolean active() { return remaining>0; }
    boolean visible() { return visible; }
    float remaining() { return remaining; }
    void end() { remaining=0;visible=false; }
    void reset() { end();nextScore=20; }
}

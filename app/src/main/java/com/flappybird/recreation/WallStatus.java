package com.flappybird.recreation;

/** Shared geometry for rendering and collisions; destroyed walls stay gone until recycled. */
final class WallStatus {
    private boolean destroyed;
    private int generation;
    private float openingExtra;

    void reset() { destroyed=false; openingExtra=0; generation++; }
    boolean destroy() {
        if(destroyed) return false;
        destroyed=true; return true;
    }
    boolean isDestroyed() { return destroyed; }
    int generation() { return generation; }

    void updateOpening(boolean phoneActive, float extra, float seconds, boolean actorCrossing) {
        extra=Math.max(0,extra);
        if(phoneActive) openingExtra=extra;
        // Do not close a wall on the character when the ten-second bonus expires.
        else if(!actorCrossing) openingExtra=Math.max(0,openingExtra-extra*Math.max(0,seconds)/.35f);
    }
    float top(float base) { return base-openingExtra/2f; }
    float bottom(float base) { return base+openingExtra/2f; }
    float extra() { return openingExtra; }
}

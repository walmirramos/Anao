package com.flappybird.recreation;

import java.util.Random;

/**
 * Gameplay state advanced only by foreground gameplay time.
 * The first three appearances are SAYAJIN, CELULAR, BAZUCA, including missed items.
 * collectibleKind() is non-null only while an item is visible; activeKind() only
 * while its collected effect is active. Callers choose the later score gap (10-25).
 */
final class PowerUpState {
    enum Kind { SAYAJIN, CELULAR, BAZUCA }

    private static final Kind[] KINDS=Kind.values();
    private final Random random;
    private int nextScore=20;
    private int appearances;
    private Kind collectible, active, previousAppearance;
    private float remaining;

    PowerUpState() { this(new Random()); }
    PowerUpState(Random random) {
        if(random==null) throw new IllegalArgumentException("random must not be null");
        this.random=random;
    }

    boolean shouldSpawn(int score) {
        return score>=nextScore && !visible() && !active();
    }

    void spawned(int score, int gap) {
        if(!shouldSpawn(score)) return;
        if(appearances<KINDS.length) {
            collectible=KINDS[appearances];
        } else {
            // Pick uniformly from the other two kinds, never repeating an appearance.
            int choice=random.nextInt(KINDS.length-1);
            if(choice>=previousAppearance.ordinal()) choice++;
            collectible=KINDS[choice];
        }
        previousAppearance=collectible;
        appearances++;
        long threshold=(long)score+Math.max(10,gap);
        nextScore=(int)Math.min(Integer.MAX_VALUE,threshold);
    }

    boolean collect() {
        if(!visible() || active()) return false;
        active=collectible;
        collectible=null;
        remaining=10f;
        return true;
    }

    void miss() { collectible=null; }

    void tick(float seconds) {
        if(seconds>0) remaining=Math.max(0,remaining-seconds);
        if(remaining==0) active=null;
    }

    boolean active() { return remaining>0 && active!=null; }
    boolean active(Kind kind) { return active() && active==kind; }
    boolean visible() { return collectible!=null; }
    float remaining() { return remaining; }
    Kind collectibleKind() { return collectible; }
    Kind activeKind() { return active() ? active : null; }

    void end() { remaining=0;active=null;collectible=null; }

    void reset() {
        end();nextScore=20;appearances=0;previousAppearance=null;
    }
}

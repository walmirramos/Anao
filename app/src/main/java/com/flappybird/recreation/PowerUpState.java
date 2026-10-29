package com.flappybird.recreation;

import java.util.Random;

/**
 * Shield stores one wall hit indefinitely; drill uses foreground time; bazooka stores three rounds.
 * The first three appearances are SAYAJIN, CELULAR, BAZUCA, including missed items.
 * collectibleKind() is non-null only while an item is visible; activeKind() only
 * while its collected effect is active. Factory bonuses unlock at 3; callers choose later gaps (4-7).
 */
final class PowerUpState {
    enum Kind { SAYAJIN, CELULAR, BAZUCA }

    private static final Kind[] KINDS=Kind.values();
    private final Random random;
    private int nextScore=3;
    private int appearances;
    private Kind collectible, active, previousAppearance;
    private float remaining;
    private int rounds;
    private boolean shotInFlight,shield;

    PowerUpState() { this(new Random()); }
    PowerUpState(Random random) {
        if(random==null) throw new IllegalArgumentException("random must not be null");
        this.random=random;
    }

    boolean shouldSpawn(int score) {
        return score>=nextScore && !visible() && !hasPower();
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
        long threshold=(long)score+Math.max(4,gap);
        nextScore=(int)Math.min(Integer.MAX_VALUE,threshold);
    }

    boolean collect() {
        if(!visible() || hasPower()) return false;
        if(collectible==Kind.SAYAJIN){shield=true;collectible=null;return true;}
        active=collectible;collectible=null;
        rounds=active==Kind.BAZUCA?3:0;shotInFlight=false;
        remaining=active==Kind.BAZUCA?0:10f;return true;
    }

    void miss() { collectible=null; }

    void tick(float seconds) {
        if(active==Kind.BAZUCA)return;
        if(seconds>0 && Float.isFinite(seconds)) remaining=Math.max(0,remaining-seconds);
        if(remaining==0) active=null;
    }

    private boolean hasPower() {return active!=null && (active==Kind.BAZUCA ? rounds>0 || shotInFlight : remaining>0);}
    boolean active() {return shield || hasPower();}
    boolean active(Kind kind) {return kind==Kind.SAYAJIN ? shield : hasPower() && active==kind;}
    boolean visible() {return collectible!=null;}
    float remaining() {return remaining;}
    Kind collectibleKind() {return collectible;}
    Kind activeKind() {return hasPower()?active:shield?Kind.SAYAJIN:null;}
    boolean consumeShield() {if(!shield)return false;shield=false;return true;}

    int rounds() {return rounds;}
    boolean fireShot() {
        if(active!=Kind.BAZUCA || rounds<=0 || shotInFlight)return false;
        rounds--;shotInFlight=true;return true;
    }
    void resolveShot() {
        shotInFlight=false;if(active==Kind.BAZUCA && rounds==0)active=null;
    }
    void end() { remaining=0;rounds=0;shotInFlight=false;shield=false;active=null;collectible=null; }

    void reset() {
        end();nextScore=3;appearances=0;previousAppearance=null;
    }
}

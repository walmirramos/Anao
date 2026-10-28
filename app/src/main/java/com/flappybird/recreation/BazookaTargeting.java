package com.flappybird.recreation;

/** Uses the actual scaled hitbox, including custom character size and reverse play. */
final class BazookaTargeting {
    private BazookaTargeting() {}

    static boolean ahead(float actorLeft, float actorRight, float wallLeft, float wallRight, boolean reverse) {
        return reverse ? wallLeft<actorRight : wallRight>actorLeft;
    }

    static boolean imminent(float actorLeft, float actorRight, float wallLeft, float wallRight,
                            boolean reverse, float margin) {
        if(!ahead(actorLeft,actorRight,wallLeft,wallRight,reverse)) return false;
        float clearance=reverse ? actorLeft-wallRight : wallLeft-actorRight;
        return clearance<=Math.max(0,margin);
    }
}

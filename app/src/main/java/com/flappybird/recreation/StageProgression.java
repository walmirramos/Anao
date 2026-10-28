package com.flappybird.recreation;

final class StageProgression {
    static final int ENVIRONMENTS=6;
    private StageProgression() {}
    static int environment(int nencoins, int startingScene) {
        if(nencoins>=40) return 5;
        if(nencoins>=20) return 4;
        return startingScene;
    }
}

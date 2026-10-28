package com.flappybird.recreation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;

/** Heavy bitmap preparation is done off the UI thread behind the loading screen. */
final class GameAssets {
    final Bitmap day, night, ground;
    final Bitmap[] characters, medals, walls=new Bitmap[WorldArt.WALL_STYLES], environments=new Bitmap[StageProgression.ENVIRONMENTS];
    final boolean[][] wallMasks=new boolean[WorldArt.WALL_STYLES][];

    GameAssets(Resources resources) {
        day=WorldArt.city(resources,false);night=WorldArt.city(resources,true);
        ground=WorldArt.ground();characters=CharacterSprites.loadVariants(resources);
        medals=MedalArt.load(resources);
        for(int style=0;style<walls.length;style++) {
            walls[style]=WorldArt.wall(style);
            int[] pixels=new int[52*320];walls[style].getPixels(pixels,0,52,0,0,52,320);
            wallMasks[style]=new boolean[pixels.length];
            for(int i=0;i<pixels.length;i++) wallMasks[style][i]=Color.alpha(pixels[i])>128;
        }
        for(int i=0;i<environments.length;i++) environments[i]=WorldArt.environment(i);
    }

    void recycle() {
        day.recycle();night.recycle();ground.recycle();
        for(Bitmap b:medals)b.recycle();
        for(Bitmap b:characters)b.recycle();
        for(Bitmap b:walls)b.recycle();
        for(Bitmap b:environments)b.recycle();
    }
}

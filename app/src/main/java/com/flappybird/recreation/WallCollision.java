package com.flappybird.recreation;

/** Maps the actor's rectangular hitbox to a wall's actual opaque native pixels. */
final class WallCollision {
    static final int WIDTH=52, HEIGHT=320;
    private WallCollision() {}

    static boolean intersects(boolean[] solid, float left, float top, float right, float bottom,
                              float wallX, float wallY, float width, float height, boolean flipY) {
        if (width<=0 || height<=0 || right<=wallX || left>=wallX+width ||
                bottom<=wallY || top>=wallY+height) return false;
        int x0=Math.max(0,(int)Math.floor((left-wallX)/width*WIDTH));
        int x1=Math.min(WIDTH,(int)Math.ceil((right-wallX)/width*WIDTH));
        int y0=Math.max(0,(int)Math.floor((top-wallY)/height*HEIGHT));
        int y1=Math.min(HEIGHT,(int)Math.ceil((bottom-wallY)/height*HEIGHT));
        for(int y=y0;y<y1;y++) {
            int row=(flipY ? HEIGHT-1-y : y)*WIDTH;
            for(int x=x0;x<x1;x++) if(solid[row+x]) return true;
        }
        return false;
    }
}

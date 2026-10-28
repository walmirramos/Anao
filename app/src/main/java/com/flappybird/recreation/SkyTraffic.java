package com.flappybird.recreation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

/** One occasional background airplane, in native 288x512 coordinates, never a tiled texture. */
final class SkyTraffic {
    private final Random random;
    private final Path silhouette=new Path();
    private final Paint aircraftPaint=new Paint();
    private float untilNext, x, y, speed;
    private int direction;
    private boolean visible;

    SkyTraffic() { this(new Random()); }

    SkyTraffic(Random random) {
        this.random=random;
        aircraftPaint.setAntiAlias(false);
        aircraftPaint.setStyle(Paint.Style.FILL);
        silhouette.moveTo(0,4);silhouette.lineTo(10,4);
        silhouette.lineTo(14,-3);silhouette.lineTo(18,-3);silhouette.lineTo(17,4);
        silhouette.lineTo(28,4);silhouette.lineTo(31,6);silhouette.lineTo(17,7);
        silhouette.lineTo(20,14);silhouette.lineTo(16,14);silhouette.lineTo(10,7);
        silhouette.lineTo(1,7);silhouette.lineTo(-2,2);silhouette.lineTo(1,2);silhouette.close();
        reset();
    }

    void reset() {
        visible=false;
        untilNext=18+random.nextFloat()*10;
        x=0;y=0;speed=0;
    }

    /** Call only while PLAYING; not ticking automatically freezes all scheduling during pauses. */
    void tick(float seconds, boolean reverse) {
        if (seconds<=0 || Float.isNaN(seconds) || Float.isInfinite(seconds)) return;
        float movement=seconds;
        if (!visible) {
            untilNext-=seconds;
            if (untilNext>0) return;
            movement=-untilNext;
            direction=reverse ? 1 : -1;
            x=direction>0 ? -40 : 328;
            y=52+random.nextInt(75);
            speed=38+random.nextFloat()*14;
            visible=true;
        }
        x+=speed*direction*movement;
        if (x < -40 || x > 328) {
            visible=false;
            untilNext=35+random.nextFloat()*25;
        }
    }

    /** The canvas must be scaled once to native space, outside any background tile loop. */
    void draw(Canvas canvas, Paint paint) {
        if (!visible) return;
        int opacity=160*paint.getAlpha()/255;
        canvas.save();
        canvas.translate(Math.round(x),Math.round(y));
        canvas.scale(direction,1);
        aircraftPaint.setColor(((opacity/3)<<24)|0x00eee6d1);
        canvas.drawRect(-23,5,-5,6,aircraftPaint);
        aircraftPaint.setColor((opacity<<24)|0x00747f87);
        canvas.drawPath(silhouette,aircraftPaint);
        aircraftPaint.setColor((opacity<<24)|0x00a5a6a0);
        canvas.drawRect(19,4,25,5,aircraftPaint);
        canvas.restore();
    }
}

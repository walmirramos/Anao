package com.flappybird.recreation;

import android.graphics.Canvas;
import android.graphics.Paint;

/** A few small red pixels at touchdown, fading out without changing the character. */
final class GroundImpactEffect {
    private final Paint paint=new Paint();
    private float age=2, x, y, scale, direction;

    void emit(float x, float y, float scale, boolean inverted) {
        this.x=x;this.y=y;this.scale=scale;direction=inverted ? 1 : -1;age=0;
    }

    void reset() { age=2; }
    void update(float seconds) { age+=seconds; }

    void draw(Canvas canvas) {
        if(age>=1.15f) return;
        paint.setColor(0xffdf272c);
        paint.setAlpha((int)(220*Math.min(1f,(1.15f-age)*2)));
        canvas.drawRect(x-4*scale,y-2*scale,x+4*scale,y,paint);
        for(int i=0;i<6;i++) {
            float side=i<3 ? -1 : 1;
            float dx=side*(2+(i%3)*2+age*(8+i*2))*scale;
            float dy=direction*Math.max(0,age*(19+i*3)-65*age*age)*scale;
            float size=(i%2+1)*scale;
            canvas.drawRect(x+dx,y+dy-size,x+dx+size,y+dy,paint);
        }
    }
}

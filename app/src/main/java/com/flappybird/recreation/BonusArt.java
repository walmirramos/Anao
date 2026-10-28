package com.flappybird.recreation;

import android.graphics.*;

final class BonusArt {
    private final Paint paint=new Paint();
    private final Path flame=new Path();
    private final Bitmap heldBazookaSprite=bazooka();

    static Bitmap banana() {
        // A compact native pixel sprite, independent of emoji font availability.
        String[] rows={
            "................", "...........bb...", "...........by...", "..........byy...",
            "..........byy...", ".........byyy...", ".........byyy...", "........byyyy...",
            ".......byyyyb...", "......byyyyyb...", "....bbyyyyyb....", "..bbyyyyyyb.....",
            ".byyyyyyyb......", ".byyyyybb.......", "..bbbbb.........", "................"};
        Bitmap b=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++) {
            char pixel=rows[y].charAt(x);
            if(pixel=='b')b.setPixel(x,y,0xff70442a);
            if(pixel=='y')b.setPixel(x,y,(x+y)%4==0 ? 0xffffe68c : 0xfff9c62e);
        }
        return b;
    }

    static Bitmap phone() {
        Bitmap b=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        p.setColor(0xff242b37);c.drawRect(4,0,12,16,p);c.drawRect(3,1,13,15,p);
        p.setColor(0xff7b8690);c.drawRect(4,1,12,15,p);
        p.setColor(0xff192f3d);c.drawRect(4,3,12,11,p);
        p.setColor(0xff69d4da);c.drawRect(5,4,11,10,p);
        p.setColor(0xffeef5df);c.drawRect(6,5,10,6,p);c.drawRect(6,7,9,8,p);
        p.setColor(0xff242b37);c.drawRect(6,1,10,2,p);
        p.setColor(0xffffd46a);c.drawRect(7,12,9,14,p);
        p.setColor(0xffd8e1df);c.drawRect(4,2,5,5,p);
        return b;
    }

    static Bitmap bazooka() {
        Bitmap b=Bitmap.createBitmap(20,16,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        p.setColor(0xff242a2b);c.drawRect(0,3,5,12,p);c.drawRect(3,4,20,11,p);
        c.drawRect(7,1,12,5,p);c.drawRect(8,10,12,16,p);c.drawRect(12,10,15,13,p);
        p.setColor(0xff6c793d);c.drawRect(1,4,4,11,p);c.drawRect(4,5,18,10,p);
        p.setColor(0xffa9b164);c.drawRect(4,5,17,6,p);c.drawRect(1,4,3,6,p);
        p.setColor(0xff455330);c.drawRect(4,9,18,10,p);c.drawRect(9,11,11,15,p);
        p.setColor(0xffb9bea1);c.drawRect(8,2,11,3,p);c.drawRect(17,5,18,10,p);
        p.setColor(0xff101b21);c.drawRect(18,5,20,10,p);
        p.setColor(0xff889179);c.drawRect(18,6,19,9,p);
        return b;
    }

    void heldBazooka(Canvas canvas, float x, float y, float scale, boolean reverse) {
        int saved=canvas.save();canvas.translate(x,y);
        canvas.scale(reverse ? -scale : scale,scale);
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);paint.setFilterBitmap(false);
        canvas.drawBitmap(heldBazookaSprite,5,5,paint);
        canvas.restoreToCount(saved);
        paint.setColor(Color.WHITE);paint.setAlpha(255);
    }

    void rocket(Canvas canvas, float x, float y, float scale, boolean reverse) {
        int saved=canvas.save();canvas.translate(x,y);
        canvas.scale(reverse ? -scale : scale,scale);
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        paint.setColor(0xff333832);canvas.drawRect(-6,-3,4,3,paint);
        paint.setColor(0xff9da765);canvas.drawRect(-5,-2,3,2,paint);
        paint.setColor(0xffe8e2ba);canvas.drawRect(3,-2,6,2,paint);canvas.drawRect(6,-1,8,1,paint);
        paint.setColor(0xffe7b948);canvas.drawRect(-10,-1,-6,1,paint);
        paint.setColor(0xfff8e39a);canvas.drawRect(-8,-1,-6,1,paint);
        paint.setColor(0xff6b7547);canvas.drawRect(-6,-4,-3,-2,paint);canvas.drawRect(-6,2,-3,4,paint);
        canvas.restoreToCount(saved);
        paint.setColor(Color.WHITE);paint.setAlpha(255);
    }

    void aura(Canvas canvas, float x, float y, float scale, double time) {
        int pulse=(int)((Math.sin(time*7)+1)*1.5);
        int saved=canvas.save();
        canvas.translate(x,y);canvas.scale(scale,scale);
        paint.setAntiAlias(false);paint.setFilterBitmap(false);
        paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(0);

        // A compact stepped teardrop, centered on the face rather than leaning sideways.
        // The outline is reused for the nested glow: no paths or paints are allocated per frame.
        flame.reset();flame.moveTo(-14,22);
        flame.lineTo(-19,22);flame.lineTo(-19,16);flame.lineTo(-23,16);
        flame.lineTo(-23,-4);flame.lineTo(-20,-4);flame.lineTo(-20,-15);
        flame.lineTo(-16,-15);flame.lineTo(-16,-23);flame.lineTo(-11,-23);
        flame.lineTo(-11,-28-pulse);flame.lineTo(-6,-28-pulse);
        flame.lineTo(-6,-33-pulse);flame.lineTo(0,-33-pulse);
        flame.lineTo(0,-30-pulse);flame.lineTo(6,-30-pulse);
        flame.lineTo(6,-25);flame.lineTo(12,-25);flame.lineTo(12,-20);
        flame.lineTo(17,-20);flame.lineTo(17,-11);flame.lineTo(21,-11);
        flame.lineTo(21,1);flame.lineTo(23,1);flame.lineTo(23,15);
        flame.lineTo(19,15);flame.lineTo(19,20);flame.lineTo(13,20);
        flame.lineTo(13,23);flame.lineTo(-14,23);flame.close();

        paint.setColor(0x38efaa16);canvas.drawPath(flame,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);
        paint.setColor(0xb8ffd95d);canvas.drawPath(flame,paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.save();canvas.translate(0,2);canvas.scale(.84f,.86f);
        paint.setColor(0x67ffd84c);canvas.drawPath(flame,paint);
        canvas.translate(0,2);canvas.scale(.75f,.78f);
        paint.setColor(0x40fff5ae);canvas.drawPath(flame,paint);
        canvas.restore();

        // Small upward sparks stay near the envelope, leaving the pipe gaps easy to read.
        for(int i=0;i<6;i++) {
            int side=(i%2==0 ? -1 : 1);
            float px=side*(23+i%3);
            float py=19-(int)((time*30+i*9)%54);
            paint.setColor(i%2==0 ? 0xe6ffe889 : 0xd9ffcf42);
            canvas.drawRect(px,py,px+2,py+3,paint);
            if(i%3==0)canvas.drawRect(px,py+3,px+1,py+5,paint);
        }
        canvas.restoreToCount(saved);
        paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(0);
        paint.setColor(Color.WHITE);paint.setAlpha(255);
    }
}

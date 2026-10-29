package com.flappybird.recreation;

import android.graphics.*;

final class BonusArt {
    private final Paint paint=new Paint();
    private final Path flame=new Path();
    private final Bitmap heldBazookaSprite=bazooka();

    static Bitmap banana() { // Tire: ten seconds of protective power.
        Bitmap b=Bitmap.createBitmap(20,20,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();p.setColor(0xff111c26);c.drawCircle(10,10,9,p);
        p.setColor(0xff647783);c.drawCircle(10,10,7,p);p.setColor(0xff24333d);c.drawCircle(10,10,5,p);
        p.setColor(0xffc6d8dd);c.drawCircle(10,10,3,p);p.setColor(0xff364b58);c.drawCircle(10,10,1,p);
        p.setColor(0xffa4b6bf);for(int y=3;y<17;y+=4){c.drawRect(2,y,4,y+2,p);c.drawRect(16,y,18,y+2,p);}
        return b;
    }
    static Bitmap phone() { // Cordless industrial screwdriver.
        Bitmap b=Bitmap.createBitmap(22,20,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();p.setColor(0xff132532);c.drawRect(1,2,16,10,p);
        p.setColor(0xffe4b735);c.drawRect(2,3,14,9,p);p.setColor(0xffe8d079);c.drawRect(3,3,13,5,p);
        p.setColor(0xff6b8190);c.drawRect(14,4,18,8,p);p.setColor(0xffd0dce1);c.drawRect(18,5,22,7,p);
        p.setColor(0xff263743);c.drawRect(6,9,12,17,p);c.drawRect(3,16,14,20,p);
        p.setColor(0xffc7a02b);c.drawRect(7,10,10,16,p);p.setColor(0xff58c8a7);c.drawRect(5,17,8,19,p);
        return b;
    }
    static Bitmap workbook() {
        Bitmap b=Bitmap.createBitmap(20,24,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint();
        p.setColor(0xff102b46);c.drawRect(1,0,19,24,p);p.setColor(0xff245b87);c.drawRect(3,1,18,23,p);
        p.setColor(0xffd3bd75);c.drawRect(5,4,15,6,p);c.drawRect(7,8,13,13,p);
        p.setColor(0xfff4e5a4);c.drawRect(9,7,11,14,p);c.drawRect(6,10,14,11,p);
        p.setColor(0xffcee0e7);c.drawRect(5,17,15,18,p);c.drawRect(7,20,13,21,p);return b;
    }
    void crown(Canvas c,float x,float y,float s) {
        int saved=c.save();c.translate(x,y);c.scale(s,s);paint.setColor(0xff7f4d13);
        c.drawRect(-13,-7,13,3,paint);paint.setColor(0xffffce45);c.drawRect(-12,-5,12,2,paint);
        c.drawRect(-12,-12,-8,-3,paint);c.drawRect(-2,-15,2,-3,paint);c.drawRect(8,-12,12,-3,paint);
        paint.setColor(0xffffefac);c.drawRect(-11,-4,11,-2,paint);paint.setColor(0xffed5755);c.drawRect(-2,-4,2,0,paint);
        c.restoreToCount(saved);paint.setColor(Color.WHITE);
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

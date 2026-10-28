package com.flappybird.recreation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/** The same pixel portrait appears inside each of the four award tiers. */
final class MedalArt {
    private MedalArt() {}

    static Bitmap[] load(Resources resources) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap portrait = BitmapFactory.decodeResource(resources, R.drawable.trump_medal_face, options);
        int left=portrait.getWidth(), top=portrait.getHeight(), right=-1, bottom=-1;
        int[] row = new int[portrait.getWidth()];
        for (int y=0; y<portrait.getHeight(); y++) {
            portrait.getPixels(row,0,row.length,0,y,row.length,1);
            for (int x=0; x<row.length; x++) if (Color.alpha(row[x])>64) {
                left=Math.min(left,x);top=Math.min(top,y);
                right=Math.max(right,x);bottom=Math.max(bottom,y);
            }
        }
        if (right<left) throw new IllegalStateException("Medal portrait is empty");
        Rect source=new Rect(left,top,right+1,bottom+1);
        float fit=Math.min(32f/source.width(),32f/source.height());
        int w=Math.max(1,Math.round(source.width()*fit)), h=Math.max(1,Math.round(source.height()*fit));
        Rect destination=new Rect(24-w/2,24-h/2,24-w/2+w,24-h/2+h);
        int[] colors={0xffdeedf2,0xffefbd4f,0xffa9b7c1,0xffbb7d4b};
        Bitmap[] medals=new Bitmap[4];
        Paint paint=new Paint();paint.setFilterBitmap(false);paint.setAntiAlias(false);
        for (int tier=0; tier<medals.length; tier++) {
            medals[tier]=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(medals[tier]);
            paint.setColor(0xff302b32);canvas.drawCircle(24,24,23,paint);
            paint.setColor(colors[tier]);canvas.drawCircle(24,24,21,paint);
            paint.setColor(0xff6e5343);canvas.drawCircle(24,24,18,paint);
            paint.setColor(0xfff1d598);canvas.drawCircle(24,24,17,paint);
            canvas.drawBitmap(portrait,source,destination,paint);
            paint.setColor(0xfffff1cf);canvas.drawRect(11,7,16,9,paint);
            canvas.drawRect(7,11,9,16,paint);
            paint.setColor(0xff5a443a);canvas.drawRect(33,39,37,41,paint);
        }
        portrait.recycle();
        return medals;
    }
}

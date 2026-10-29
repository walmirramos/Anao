package com.flappybird.recreation;

import android.content.res.Resources;
import android.graphics.*;

/** Small cached UI bitmaps; generated originals remain unmodified in the resources. */
final class BrandArt {
    private static Bitmap backdrop, logo;
    private BrandArt() {}

    static synchronized void load(Resources resources) {
        if(backdrop!=null && logo!=null) return;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        backdrop=WorldArt.city(resources,false);
        Paint pixels=new Paint();pixels.setFilterBitmap(false);
        Bitmap source;
        source=BitmapFactory.decodeResource(resources,R.drawable.corre_logo,options);
        int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        int[] row=new int[source.getWidth()];
        for(int y=0;y<source.getHeight();y++) {
            source.getPixels(row,0,row.length,0,y,row.length,1);
            for(int x=0;x<row.length;x++) if(Color.alpha(row[x])>128) {
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
        }
        if(right<left) throw new IllegalStateException("Logo is empty");
        Rect bounds=new Rect(left,top,right+1,bottom+1);
        float fit=Math.min(252f/bounds.width(),200f/bounds.height());
        logo=Bitmap.createBitmap(Math.max(1,Math.round(bounds.width()*fit)),
                Math.max(1,Math.round(bounds.height()*fit)),Bitmap.Config.ARGB_8888);
        new Canvas(logo).drawBitmap(source,bounds,new Rect(0,0,logo.getWidth(),logo.getHeight()),pixels);
        source.recycle();
    }

    static void background(Canvas canvas, Paint pixels) { canvas.drawBitmap(backdrop,0,0,pixels); }
    static void logo(Canvas canvas, float y, Paint pixels) {
        canvas.drawBitmap(logo,(288-logo.getWidth())/2f,y,pixels);
    }
    static Bitmap menuLogo(Resources resources) {
        load(resources);
        float fit=Math.min(220f/logo.getWidth(),72f/logo.getHeight());
        return Bitmap.createScaledBitmap(logo,Math.max(1,Math.round(logo.getWidth()*fit)),
                Math.max(1,Math.round(logo.getHeight()*fit)),false);
    }
}

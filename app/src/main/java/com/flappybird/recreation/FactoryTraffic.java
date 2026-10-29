package com.flappybird.recreation;
import android.content.res.Resources;
import android.graphics.*;
/** Detailed 16-bit carrier sprites, scenery only. */
final class FactoryTraffic {
    private float time;private final Paint p=new Paint();private final Bitmap car;
    private final ColorMatrixColorFilter night=new ColorMatrixColorFilter(new float[]{
        .45f,0,0,0,12, 0,.32f,0,0,0, 0,0,.47f,0,4, 0,0,0,1,0});
    FactoryTraffic(Resources resources){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;
        o.inPreferredConfig=Bitmap.Config.ARGB_8888;o.inMutable=true;o.inPremultiplied=true;
        Bitmap source=BitmapFactory.decodeResource(resources,R.drawable.factory_car_16bit,o);
        int l=source.getWidth(),t=source.getHeight(),r=0,b=0;int[] row=new int[source.getWidth()];
        for(int y=0;y<source.getHeight();y++){
            source.getPixels(row,0,row.length,0,y,row.length,1);
            for(int x=0;x<row.length;x++)if(Color.alpha(row[x])>128){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
        }
        Bitmap crop=Bitmap.createBitmap(source,l,t,r-l+1,b-t+1);
        car=Bitmap.createScaledBitmap(crop,96,Math.max(1,Math.round(96f*crop.getHeight()/crop.getWidth())),false);
        if(crop!=source)crop.recycle();source.recycle();p.setFilterBitmap(false);p.setAntiAlias(false);
    }
    void tick(float dt){time=(time+Math.max(0,dt))%1000;}
    private void box(Canvas c,int color,float x,float y,float r,float b){p.setColor(color);c.drawRect(x,y,r,b,p);}
    void draw(Canvas c,float width,float height,boolean macabre){
        c.save();c.scale(width/288f,height/512f);
        if(macabre){
            // Slow emergency glow and low fog; walls/player stay bright and legible.
            int a=55+(int)(15*Math.sin(time*1.2));
            for(int y=278;y<397;y+=14)box(c,Color.argb(20,104,25,60),0,y,288,y+6);
            for(int x=29;x<288;x+=87){
                box(c,Color.argb(a,220,31,43),x-8,80,x+11,94);
                box(c,0xffba3d42,x-2,85,x+5,89);
            }
            box(c,0xffb6b6aa,222,245,224,247);box(c,0xffb6b6aa,228,245,230,247);
        }
        p.setColorFilter(macabre?night:null);p.setColor(Color.WHITE);
        for(int k=0;k<4;k++){
            float x=((k*124+time*10)%496)-124;
            c.drawBitmap(car,x,345,p);
        }
        p.setColorFilter(null);
        if(!macabre)for(int k=0;k<3;k++){
            float x=35+k*100;int frame=(int)(time*5+k)%5;
            if(frame<2){box(c,0xffffee9b,x+28,323,x+30,326);box(c,0xffffb94e,x+32,319,x+34,322);}
        }
        c.restore();p.setColor(Color.WHITE);
    }
}

package com.flappybird.recreation;
import android.content.res.Resources;
import android.graphics.*;
/** Industrial pixel geometry cached at native resolution. */
final class WorldArt {
    static final int WALL_STYLES=6;
    private WorldArt() {}
    private static void rect(Canvas c,Paint p,int color,float x,float y,float r,float b) {
        p.setColor(color);c.drawRect(x,y,r,b,p);
    }
    static Bitmap city(Resources res,boolean night) {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inMutable=true;options.inPremultiplied=true;
        Bitmap source=BitmapFactory.decodeResource(res,R.drawable.factory_16bit,options);
        Bitmap b=Bitmap.createBitmap(288,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();p.setFilterBitmap(false);
        if(night)p.setColorFilter(new ColorMatrixColorFilter(new float[]{
            .5f,0,0,0,0, 0,.55f,0,0,2, 0,0,.72f,0,7, 0,0,0,1,0}));
        c.drawBitmap(source,null,new Rect(0,0,288,512),p);source.recycle();return b;
    }
    static Bitmap wall(int variant) {
        Bitmap b=Bitmap.createBitmap(52,320,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        int[] steel={0xff597784,0xff66717d,0xff91734c,0xff3f7d80,0xff6f697e,0xff8a533b};
        rect(c,p,0xff17252e,0,0,52,320);rect(c,p,steel[variant%6],3,3,49,320);
        rect(c,p,0xffa2b1b4,5,6,9,320);rect(c,p,0xff293f4c,42,6,47,320);
        for(int y=22;y<320;y+=38) {
            rect(c,p,0xff243b46,3,y,49,y+4);
            rect(c,p,0xffc9d1c6,12,y+10,15,y+13);rect(c,p,0xffc9d1c6,36,y+10,39,y+13);
            if(variant%3==1) {
                rect(c,p,0xff172b36,19,y+10,33,y+29);
                for(int z=0;z<15;z+=5) rect(c,p,0xff80969d,20,y+12+z,32,y+14+z);
            } else if(variant%3==2) {
                rect(c,p,0xff253844,22,y+5,31,y+38);rect(c,p,0xffacb9b9,24,y+6,27,y+38);
            }
        }
        // Safety collars and warning stripes stay within the original collision footprint.
        for(int y=0;y<310;y+=104) {
            rect(c,p,0xffe5b844,0,y,52,y+13);
            for(int x=-12;x<52;x+=16) {
                Path q=new Path();q.moveTo(x,y);q.lineTo(x+7,y);q.lineTo(x+19,y+13);q.lineTo(x+12,y+13);q.close();
                p.setColor(0xff202a32);c.save();c.clipRect(0,y,52,y+13);c.drawPath(q,p);c.restore();
            }
        }
        // Machined lip, recessed channel, bolts and restrained metal highlights.
        rect(c,p,0xff152b38,0,0,52,5);rect(c,p,0xffbacdd2,2,1,50,2);
        rect(c,p,0xff3b515e,10,17,13,320);rect(c,p,0xffadc1c7,13,18,14,320);
        rect(c,p,0xff293f4b,38,17,41,320);
        for(int y=18;y<310;y+=26) {
            rect(c,p,0xff182d38,4,y,8,y+4);rect(c,p,0xffd0dce0,5,y+1,7,y+2);
            rect(c,p,0xff182d38,44,y,48,y+4);rect(c,p,0xffd0dce0,45,y+1,47,y+2);
        }
        if(variant>=3) {
            rect(c,p,0xffa9c044,17,58,36,74);rect(c,p,0xff24313b,22,62,31,70);
            for(int y=80;y<310;y+=63) rect(c,p,0xffb67642,10,y,20,y+3);
        }
        return b;
    }
    static Bitmap environment(int variant) {
        Bitmap b=Bitmap.createBitmap(288,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        // Sector signs supplement the detailed assembly illustration.
        String[] sectors={"ESTAMPARIA","SOLDAGEM","PINTURA","MONTAGEM","QUALIDADE","EXPEDIÇÃO"};
        rect(c,p,0xff13212d,91,272,198,288);
        p=textPaint(8);p.setColor(0xffdfe9db);c.drawText(sectors[variant%6],144,283,p);
        if(variant>=2) {
            rect(c,p,0xff324854,8,300,25,345);rect(c,p,0xff75cdbb,11,305,22,316);
            rect(c,p,0xffe9ad45,12,327,16,331);rect(c,p,0xffb25744,19,327,23,331);
        }
        return b;
    }
    static void brand(Canvas c, float x, float y, float scale) {
        Paint p=textPaint(12*scale);
        p.setColor(0xff302b32);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*scale);
        c.drawText("Dubbox 2026",x,y,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffffd471);c.drawText("Dubbox 2026",x,y,p);
        p.setTextSize(7*scale);p.setColor(0xffe1d5c1);c.drawText("ANÃO DE FIRMA 1.4.2",x,y+13*scale,p);
    }

    static Bitmap ground() {
        Bitmap b=Bitmap.createBitmap(336,112,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();c.drawColor(0xff25333d);
        rect(c,p,0xffcba43b,0,0,336,8);
        for(int x=0;x<336;x+=16) rect(c,p,0xff15212b,x,0,x+8,8);
        rect(c,p,0xff91a4ab,0,10,336,16);
        for(int x=0;x<336;x+=20) {
            rect(c,p,0xff111f28,x,20,x+14,49);rect(c,p,0xff697e88,x+3,22,x+7,47);
        }
        rect(c,p,0xff111f28,0,52,336,59);return b;
    }

    static Bitmap button(String first, String second) {
        Bitmap b=Bitmap.createBitmap(116,70,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b); Paint p=textPaint(16);
        p.setColor(0xff302b32);c.drawRect(0,0,116,70,p);
        p.setColor(0xfff5cb76);c.drawRect(3,3,113,64,p);
        p.setColor(0xffb6643c);c.drawRect(3,60,113,67,p);
        p.setColor(0xff302b32);
        c.drawText(first,58,29,p);c.drawText(second,58,50,p);
        return b;
    }

    static Bitmap panel() {
        Bitmap b=Bitmap.createBitmap(238,126,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b); Paint p=textPaint(12);
        p.setColor(0xff302b32);c.drawRect(0,0,238,126,p);
        p.setColor(0xffead3a4);c.drawRect(3,3,235,123,p);
        p.setColor(0xff6d4535);
        c.drawText("MEDALHA",54,27,p);
        c.drawText("COINS",177,30,p);
        c.drawText("RECORDE",177,74,p);
        return b;
    }

    static Bitmap message(boolean gameOver) {
        Bitmap b=Bitmap.createBitmap(204,62,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b); Paint p=textPaint(gameOver ? 24 : 22);
        String title=gameOver ? "FIM DE JOGO" : "PREPARADO?";
        p.setColor(0xff302b32);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);
        c.drawText(title,102,29,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffffd471);c.drawText(title,102,29,p);
        if (!gameOver) {
            p.setTextSize(12);p.setColor(0xff302b32);
            c.drawText("TOQUE PARA CORRER",103,53,p);
            p.setColor(Color.WHITE);c.drawText("TOQUE PARA CORRER",102,52,p);
        }
        return b;
    }

    static Bitmap credit() {
        Bitmap b=Bitmap.createBitmap(126,18,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b); Paint p=textPaint(12);
        p.setColor(0xff302b32);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
        c.drawText("Dubbox 2026",63,13,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffffd471);
        c.drawText("Dubbox 2026",63,13,p);
        return b;
    }

    static Paint textPaint(float size) {
        Paint p=new Paint(); p.setAntiAlias(false);
        p.setTypeface(PixelFont.current());
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        return p;
    }

    static void coinLabel(Canvas c, float x, float y, float scale) {
        Paint p=textPaint(10*scale);
        p.setColor(0xff302b32);c.drawText("COINS",x+scale,y+scale,p);
        p.setColor(0xffffd471);c.drawText("COINS",x,y,p);
    }
}

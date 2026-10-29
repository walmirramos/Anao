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
        Bitmap b=Bitmap.createBitmap(288,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        c.drawColor(night?0xff111b29:0xff263a49);
        // Sawtooth roof, skylights, overhead beams and suspended services.
        for(int x=0;x<288;x+=48) {
            Path roof=new Path();roof.moveTo(x,70);roof.lineTo(x+36,38);roof.lineTo(x+36,70);roof.close();
            p.setColor(0xff506677);c.drawPath(roof,p);
            rect(c,p,0xffa0ccd0,x+30,44,x+34,67);
        }
        for(int y=72;y<310;y+=72) rect(c,p,0xff172634,0,y,288,y+7);
        for(int x=10;x<288;x+=72) {
            rect(c,p,0xff152534,x,72,x+9,400);rect(c,p,0xff6d8190,x+2,74,x+4,400);
            rect(c,p,0xffd6e4ce,x+16,91,x+58,94);rect(c,p,0xff3a525e,x+14,94,x+60,99);
        }
        rect(c,p,0xff14232e,0,287,288,400);
        // Automotive assembly line with unfinished vehicle bodies.
        for(int x=-20;x<288;x+=100) {
            rect(c,p,0xff596d77,x,331,x+72,350);rect(c,p,0xff91a9af,x+14,318,x+55,335);
            rect(c,p,0xff1a3442,x+20,321,x+34,331);rect(c,p,0xff1a3442,x+38,321,x+50,331);
            rect(c,p,0xff17232b,x+8,345,x+21,357);rect(c,p,0xff17232b,x+52,345,x+65,357);
        }
        rect(c,p,0xff778994,0,365,288,373);
        for(int x=0;x<288;x+=12) rect(c,p,0xff263a43,x,368,x+6,376);
        rect(c,p,0xff0d1d2a,84,116,204,144);
        p=textPaint(13);p.setColor(0xffd5e4ef);c.drawText("STELLANTIS",144,135,p);
        p.setTextSize(7);p.setColor(0xff9bacb9);c.drawText("LINHA DE MONTAGEM",144,156,p);
        return b;
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
                for(int z=0;z<16;z+=5) rect(c,p,0xff80969d,20,y+12+z,32,y+14+z);
            } else if(variant%3==2) {
                rect(c,p,0xff253844,22,y+5,31,y+38);rect(c,p,0xffacb9b9,24,y+6,27,y+38);
            }
        }
        // Safety collars and warning stripes stay within the original collision footprint.
        for(int y=0;y<320;y+=106) {
            rect(c,p,0xffe5b844,0,y,52,y+13);
            for(int x=-12;x<52;x+=16) {
                Path q=new Path();q.moveTo(x,y);q.lineTo(x+7,y);q.lineTo(x+19,y+13);q.lineTo(x+12,y+13);q.close();
                p.setColor(0xff202a32);c.save();c.clipRect(0,y,52,y+13);c.drawPath(q,p);c.restore();
            }
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
        // Robot welding cells in six production sectors, scenery only.
        int orange=variant%2==0?0xffd78d36:0xffcd6447;
        for(int x=28;x<288;x+=160) {
            rect(c,p,0xff1b2d36,x,377,x+36,394);
            rect(c,p,orange,x+12,341,x+23,379);rect(c,p,orange,x+18,332,x+50,343);
            rect(c,p,orange,x+43,311,x+52,340);rect(c,p,0xffa7bac0,x+46,307,x+61,313);
            rect(c,p,0xffffdf78,x+59,306,x+62,310);
            rect(c,p,0xffdba54d,x+66,296,x+68,301);rect(c,p,0xffdba54d,x+65,314,x+69,316);
        }
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
        p.setTextSize(7*scale);p.setColor(0xffe1d5c1);c.drawText("ANÃO DE FIRMA 1.0",x,y+13*scale,p);
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
        c.drawText("PEÇAS",177,30,p);
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
        p.setColor(0xff302b32);c.drawText("PEÇAS",x+scale,y+scale,p);
        p.setColor(0xffffd471);c.drawText("PEÇAS",x,y,p);
    }
}

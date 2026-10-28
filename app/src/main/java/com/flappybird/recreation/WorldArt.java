package com.flappybird.recreation;

import android.content.res.Resources;
import android.graphics.*;
import java.util.Random;

/** Pixel geometry is drawn at the game's native resolution, then scaled without filtering. */
final class WorldArt {
    static final int WALL_STYLES = 6;
    private WorldArt() {}

    static Bitmap city(Resources res, boolean night) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap original = BitmapFactory.decodeResource(res, R.drawable.sao_paulo, options);
        Bitmap day = Bitmap.createBitmap(288, 512, Bitmap.Config.ARGB_8888);
        Canvas cityCanvas = new Canvas(day);
        cityCanvas.drawColor(0xff363640);
        // Keep the full skyline above the 112-pixel foreground strip.
        cityCanvas.drawBitmap(original, null, new Rect(0, 0, 288, 400), new Paint());
        original.recycle();
        if (!night) return day;
        Bitmap dark = Bitmap.createBitmap(288, 512, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(new float[]{
                .28f,0,0,0,9, 0,.32f,0,0,12, 0,0,.48f,0,28, 0,0,0,1,0}));
        new Canvas(dark).drawBitmap(day, 0, 0, paint);
        day.recycle();
        return dark;
    }

    static Bitmap wall(int variant) {
        Bitmap image = Bitmap.createBitmap(52, 320, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(image);
        Paint p = new Paint();
        int[][] palette = {{0xffb95335,0xffd9784b,0xff74392e},
                {0xff8d6961,0xffb28d7b,0xff533e40},
                {0xffc38a48,0xffe2ac65,0xff79522f}};
        int[] colors = palette[variant % palette.length];
        p.setColor(0xff453c3b); c.drawRect(0,0,52,320,p);
        Random rng = new Random(827 + variant);
        for (int row=0; row<40; row++) {
            int offset = row%2==0 ? 0 : -12;
            for (int x=offset; x<52; x+=24) {
                int y=row*8;
                p.setColor(colors[rng.nextInt(3)]); c.drawRect(x+1,y+1,x+23,y+7,p);
                p.setColor(colors[1]); c.drawRect(x+2,y+1,x+21,y+2,p);
                p.setColor(colors[2]); c.drawRect(x+2,y+6,x+22,y+7,p);
                if (rng.nextInt(9)==0) {
                    p.setColor(0xff453c3b); c.drawRect(x+12,y+2,x+13,y+5,p);
                    c.drawRect(x+13,y+4,x+15,y+5,p);
                }
            }
        }
        p.setColor(0xff302b32); c.drawRect(0,0,2,320,p); c.drawRect(50,0,52,320,p);
        // A straight concrete coping, with the same width as the wall body.
        p.setColor(0xff514d53); c.drawRect(0,0,52,6,p);
        p.setColor(0xffc9b9a0); c.drawRect(2,1,50,3,p);
        if (variant >= 3) {
            // Missing plaster, long stepped cracks and exposed mortar.
            p.setColor(0xffb8aa8d);
            c.drawRect(4,30,24,75,p);c.drawRect(4,53,36,68,p);
            c.drawRect(28,180,48,220,p);
            p.setColor(0xff302b32);
            for(int y=10;y<305;y+=7) {
                int x=22+(y/7%3)*3;
                c.drawRect(x,y,x+2,y+8,p);
                c.drawRect(x,y+5,x+6,y+7,p);
            }
            p.setColor(0xff627346);
            for(int y=80;y<310;y+=31) c.drawRect(2,y,5,y+12,p);
        }
        if (variant == 4) {
            // Jagged broken edges: collision uses these exact opaque pixels.
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            c.drawRect(0,0,11,8,p);c.drawRect(0,8,6,18,p);
            c.drawRect(37,0,52,5,p);c.drawRect(46,5,52,14,p);
            c.drawRect(0,91,5,105,p);c.drawRect(48,155,52,172,p);
            c.drawRect(0,243,7,250,p);
            p.setXfermode(null);
            p.setColor(0xff332e32);
            c.drawRect(9,122,31,128,p);c.drawRect(21,128,43,134,p);
        }
        if (variant == 5) {
            p.setColor(0xff75635c);c.drawRect(0,0,52,17,p);
            p.setColor(0xffbbb4a4);
            c.drawRect(6,20,8,320,p);c.drawRect(44,20,46,320,p);
            for(int y=28;y<320;y+=48) c.drawRect(5,y,47,y+2,p);
            for(int y : new int[]{5,91,201}) {
                p.setColor(0xffe8b84b);c.drawRect(2,y,50,y+12,p);
                p.setColor(0xff292932);
                for(int x=-10;x<52;x+=14) {
                    Path stripe=new Path();stripe.moveTo(x,y);stripe.lineTo(x+7,y);
                    stripe.lineTo(x+17,y+12);stripe.lineTo(x+10,y+12);stripe.close();
                    c.save();c.clipRect(2,y,50,y+12);c.drawPath(stripe,p);c.restore();
                }
            }
            p=textPaint(7);p.setColor(0xffffe7ad);c.drawText("EM OBRAS",26,59,p);
        }
        return image;
    }

    static Bitmap environment(int variant) {
        Bitmap b=Bitmap.createBitmap(288,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();
        if ((variant & 1) != 0 || variant>=4) {
            for(int y=0;y<282;y+=12) {
                int alpha=y<190 ? 100 : 100-(y-190)/2;
                p.setColor(Color.argb(alpha,142,126,100));c.drawRect(0,y,288,y+12,p);
            }
            p.setColor(0x407c7971);
            for(int x : new int[]{18,105,231}) {
                c.drawRect(x,204,x+28,224,p);c.drawRect(x+5,193,x+19,215,p);
                c.drawRect(x-7,177,x+14,197,p);
            }
        }
        if (variant>=4) {
            // Later stages: layered rubbish heaps, discarded tires and dirty runoff.
            p.setColor(0xff71674b);c.drawRect(0,318,288,400,p);
            Random trash=new Random(1600+variant);
            int[] bags={0xff414943,0xff57634b,0xff6d6556,0xff837258,0xff30383b};
            for(int row=0;row<4;row++) {
                int y=306+row*21;
                for(int x=-8;x<288;x+=23) {
                    int h=12+trash.nextInt(16);
                    p.setColor(bags[trash.nextInt(bags.length)]);
                    c.drawRect(x+2,y-h+5,x+23,y+14,p);c.drawRect(x+6,y-h,x+18,y+14,p);
                    p.setColor(0xffa19474);c.drawRect(x+8,y-h,x+12,y-h+3,p);
                }
            }
            p.setColor(0xff74953c);c.drawRect(15,375,144,381,p);c.drawRect(32,381,187,388,p);
            for(int x : new int[]{41,138,244}) {
                p.setColor(0xff252b2e);c.drawCircle(x,361,13,p);
                p.setColor(0xff85775d);c.drawCircle(x,361,6,p);
                p.setColor(0xff8b593e);c.drawRect(x+12,322,x+27,355,p);
                p.setColor(0xff3b3e34);c.drawRect(x+11,325,x+28,328,p);c.drawRect(x+11,346,x+28,349,p);
                p.setColor(0x906d7162);c.drawRect(x+12,287,x+27,316,p);
                c.drawRect(x+7,269,x+20,292,p);
            }
            if(variant==5) {
                p.setColor(0xffa57c33);c.drawRect(184,290,245,319,p);
                p.setColor(0xff3c3932);c.drawRect(189,296,240,300,p);
                p=textPaint(9);p.setColor(0xffffdc83);c.drawText("LIXÃO",214,312,p);
            }
            wreckCar(c,p,174,367,variant==4 ? 0xff88544b : 0xff666d73);
            if(variant==5) wreckCar(c,p,65,375,0xff6d7562);
            landfillFire(c,p,53,357,variant);
            landfillFire(c,p,149,345,variant+1);
        } else if(variant==1) {
            wreckCar(c,p,139,371,0xff8d5148);
        }
        if ((variant & 2) != 0 && variant<4) {
            p.setColor(0xff3b343a);c.drawRect(38,354,42,400,p);c.drawRect(104,354,108,400,p);
            c.drawRect(19,308,127,357,p);
            p.setColor(0xffeee1b7);c.drawRect(22,311,124,354,p);
            p.setColor(0xffa74434);c.drawRect(22,311,124,315,p);c.drawRect(22,350,124,354,p);
            p=textPaint(10);p.setColor(0xffa02f2f);c.drawText("PROIBIDO",73,330,p);
            p.setColor(0xff302b32);c.drawText("MILICIANO",73,344,p);
        }
        // Posters are scenery only; they do not change wall or ground collisions.
        if(variant==1 || variant==3) protestSign(c,170,315);
        else if(variant==5) protestSign(c,7,264);
        return b;
    }

    private static void wreckCar(Canvas c, Paint p, int x, int y, int color) {
        p.setStyle(Paint.Style.FILL);
        // Wheels on top and the crushed roof below make the overturned pose explicit.
        p.setColor(0xff252a30);c.drawCircle(x+11,y+5,6,p);c.drawCircle(x+43,y+5,6,p);
        p.setColor(0xff797e7e);c.drawCircle(x+11,y+5,2,p);c.drawCircle(x+43,y+5,2,p);
        p.setColor(0xff2c2d33);c.drawRect(x,y+7,x+56,y+18,p);
        c.drawRect(x+10,y+17,x+45,y+25,p);c.drawRect(x+17,y+24,x+39,y+27,p);
        p.setColor(color);c.drawRect(x+2,y+9,x+54,y+16,p);c.drawRect(x+12,y+16,x+43,y+23,p);
        p.setColor(0xff93a4a5);c.drawRect(x+15,y+18,x+26,y+22,p);c.drawRect(x+29,y+18,x+39,y+21,p);
        p.setColor(0xff423b3a);c.drawRect(x+32,y+9,x+34,y+15,p);c.drawRect(x+34,y+13,x+40,y+15,p);
        p.setColor(0xffc0b7a1);c.drawRect(x+1,y+14,x+7,y+16,p);c.drawRect(x+48,y+16,x+55,y+18,p);
    }

    private static void landfillFire(Canvas c, Paint p, int x, int base, int variation) {
        p.setStyle(Paint.Style.FILL);
        int tip=variation%2*5;
        p.setColor(0x65776b60);c.drawRect(x-10,base-64,x+7,base-45,p);
        c.drawRect(x-3,base-80,x+17,base-59,p);c.drawRect(x+7,base-96,x+23,base-78,p);
        p.setColor(0xff932e23);c.drawRect(x-13,base-10,x+13,base+1,p);
        c.drawRect(x-10,base-23,x+10,base-6,p);c.drawRect(x-6,base-38-tip,x-1,base-15,p);
        c.drawRect(x+4,base-43+tip,x+9,base-15,p);c.drawRect(x+10,base-27,x+14,base-6,p);
        p.setColor(0xffed6e25);c.drawRect(x-9,base-15,x+10,base,p);
        c.drawRect(x-6,base-29-tip,x-1,base-8,p);c.drawRect(x+3,base-34+tip,x+7,base-7,p);
        p.setColor(0xffffce55);c.drawRect(x-4,base-10,x+5,base,p);
        c.drawRect(x,base-22,x+4,base-5,p);
        p.setColor(0xffffe8a0);c.drawRect(x-1,base-7,x+3,base-1,p);
        c.drawRect(x-8,base-48,x-6,base-45,p);c.drawRect(x+13,base-53,x+15,base-50,p);
    }

    private static void protestSign(Canvas c, int x, int y) {
        Paint p=new Paint();p.setColor(0xff39343a);
        c.drawRect(x+10,y+39,x+14,y+77,p);c.drawRect(x+92,y+39,x+96,y+77,p);
        c.drawRect(x,y,x+111,y+43,p);
        p.setColor(0xffe8ddba);c.drawRect(x+3,y+3,x+108,y+40,p);
        p.setColor(0xffa94135);c.drawRect(x+3,y+3,x+108,y+6,p);
        p=textPaint(12);p.setColor(0xff9c352e);c.drawText("FORA",x+55,y+21,p);
        p.setTextSize(11);p.setColor(0xff302c32);c.drawText("TARCÍSIO",x+55,y+35,p);
    }

    static void brand(Canvas c, float x, float y, float scale) {
        Paint p=textPaint(12*scale);
        p.setColor(0xff302b32);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*scale);
        c.drawText("Dubbox 2026",x,y,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffffd471);c.drawText("Dubbox 2026",x,y,p);
        p.setTextSize(7*scale);p.setColor(0xffe1d5c1);c.drawText("CORRE NENGUE 1.10.1",x,y+13*scale,p);
    }

    static Bitmap ground() {
        Bitmap b=Bitmap.createBitmap(336,112,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b); Paint p=new Paint();
        p.setColor(0xff363640); c.drawRect(0,0,336,112,p);
        p.setColor(0xffb7ac9a); c.drawRect(0,0,336,7,p);
        p.setColor(0xff6b6464); c.drawRect(0,7,336,20,p);
        for(int x=0;x<336;x+=24) {p.setColor(0xff38343d);c.drawRect(x,7,x+1,20,p);}
        p.setColor(0xffe8bc62);
        for(int x=8;x<336;x+=56) c.drawRect(x,48,x+30,51,p);
        return b;
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
        c.drawText("NENCOINS",177,30,p);
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
        p.setColor(0xff302b32);c.drawText("NENCOINS",x+scale,y+scale,p);
        p.setColor(0xffffd471);c.drawText("NENCOINS",x,y,p);
    }
}

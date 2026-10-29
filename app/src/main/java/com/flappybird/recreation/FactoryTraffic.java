package com.flappybird.recreation;
import android.graphics.*;
/** Moving vehicle bodies on the distant assembly conveyor, with no collision. */
final class FactoryTraffic {
    private static final int[] COLORS={0xffc95743,0xffe2dfcc,0xff4a8ba5,0xffd4a348};
    private float time;private final Paint p=new Paint();
    void tick(float dt){time=(time+Math.max(0,dt))%1000;}
    private void box(Canvas c,int color,float x,float y,float r,float b){p.setColor(color);c.drawRect(x,y,r,b,p);}
    void draw(Canvas c,float width,float height){
        c.save();c.scale(width/288f,height/512f);
        for(int k=0;k<4;k++){
            float x=((k*104+time*11)%416)-100;float y=352;
            box(c,0xff12252e,x,y+4,x+72,y+19);box(c,COLORS[k],x+2,y+3,x+70,y+15);
            box(c,COLORS[k],x+17,y-9,x+53,y+5);box(c,0xff84b4bd,x+21,y-6,x+35,y+3);
            box(c,0xff5b8a9b,x+38,y-6,x+49,y+3);box(c,0xfff2ddaa,x+64,y+6,x+70,y+9);
            box(c,0xff18252e,x+9,y+14,x+22,y+25);box(c,0xff18252e,x+51,y+14,x+64,y+25);
            box(c,0xffa3b4b8,x+13,y+17,x+18,y+22);box(c,0xffa3b4b8,x+55,y+17,x+60,y+22);
        }
        for(int k=0;k<3;k++){
            float x=35+k*100;int frame=(int)(time*5+k)%5;
            if(frame<2){box(c,0xffffee9b,x+28,323,x+30,326);box(c,0xffffb94e,x+32,319,x+34,322);}
        }
        c.restore();
    }
}

package com.flappybird.recreation;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Arrays;
import java.util.Random;

/** Fixed-capacity screen-pixel rubble, emitted from the two mouths of a destroyed wall. */
final class WallBlastEffects {
    private static final int LIMIT=128, FLASH_LIMIT=8;
    private static final float DURATION=1.1f, FLASH_DURATION=.18f;
    private static final int[][] COLORS={
        {0xffbe5d3d,0xffd88658,0xff8d4133},
        {0xff97766d,0xffb3947f,0xff654d48},
        {0xffc28a4c,0xffdfac6a,0xff946535},
        {0xffa7896a,0xffb8aa8d,0xff6e6554},
        {0xffae6950,0xffce9672,0xff6a4b40},
        {0xffd1a644,0xffa18e62,0xff69644d}
    };
    private final float[] x=new float[LIMIT],y=new float[LIMIT],vx=new float[LIMIT],vy=new float[LIMIT];
    private final float[] gravity=new float[LIMIT],floor=new float[LIMIT],w=new float[LIMIT],h=new float[LIMIT],life=new float[LIMIT];
    private final boolean[] landed=new boolean[LIMIT];
    private final int[] colors=new int[LIMIT];
    private final float[] flashX=new float[FLASH_LIMIT],flashY=new float[FLASH_LIMIT],flashWidth=new float[FLASH_LIMIT],flashLife=new float[FLASH_LIMIT];
    private final Random random=new Random(11026);
    private int next, nextFlash;

    void clear() {
        Arrays.fill(life,0);Arrays.fill(flashLife,0);next=0;nextFlash=0;
    }

    void explode(float x, float topEdge, float bottomEdge, float width, float floorY, int wallStyle, boolean reverse) {
        if(width<=0) return;
        float scale=Math.max(.5f,width/52f);
        int[] palette=COLORS[Math.max(0,wallStyle)%COLORS.length];
        float direction=reverse ? -1 : 1;
        for(int mouth=0;mouth<2;mouth++) {
            float edge=mouth==0 ? topEdge : bottomEdge;
            int flash=nextFlash++%FLASH_LIMIT;
            flashX[flash]=x+width*.5f;flashY[flash]=edge;
            flashWidth[flash]=width;flashLife[flash]=FLASH_DURATION;
            for(int brick=0;brick<24;brick++) {
                int i=next++%LIMIT;
                w[i]=(3+random.nextInt(5))*scale;h[i]=(2+random.nextInt(3))*scale;
                this.x[i]=x+random.nextFloat()*Math.max(0,width-w[i]);
                y[i]=edge+(mouth==0 ? -1 : 1)*random.nextFloat()*16*scale;
                floor[i]=floorY;
                y[i]=Math.min(y[i],floorY-h[i]);
                vx[i]=(random.nextFloat()-.5f)*150*scale+direction*20*scale;
                vy[i]=(mouth==0 ? 15+random.nextFloat()*45 : -65-random.nextFloat()*60)*scale;
                // Reach the ground within the effect's lifetime, including high upper mouths.
                gravity[i]=Math.max(420*scale,2*(floorY-h[i]-y[i]-vy[i]*.82f)/(.82f*.82f));
                life[i]=DURATION;landed[i]=false;colors[i]=palette[random.nextInt(palette.length)];
            }
        }
    }

    void tick(float seconds, float scrollSpeed) {
        float dt=Math.max(0,seconds);
        for(int i=0;i<LIMIT;i++) if(life[i]>0) {
            life[i]-=dt;
            if(life[i]<=0) continue;
            x[i]+=(vx[i]-scrollSpeed)*dt;
            if(!landed[i]) {
                y[i]+=vy[i]*dt+gravity[i]*dt*dt*.5f;vy[i]+=gravity[i]*dt;
                if(y[i]+h[i]>=floor[i]) {
                    y[i]=floor[i]-h[i];vy[i]=0;vx[i]*=.18f;landed[i]=true;
                }
            }
        }
        for(int i=0;i<FLASH_LIMIT;i++) if(flashLife[i]>0) {
            flashLife[i]-=dt;flashX[i]-=scrollSpeed*dt;
        }
    }

    void draw(Canvas canvas, Paint paint) {
        int originalColor=paint.getColor(),originalAlpha=paint.getAlpha();
        Paint.Style originalStyle=paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<FLASH_LIMIT;i++) if(flashLife[i]>0) {
            float progress=1-flashLife[i]/FLASH_DURATION;
            float radius=flashWidth[i]*(.25f+.35f*progress);
            paint.setColor(0xffffbd39);paint.setAlpha((int)(220*(1-progress)));
            canvas.drawRect(flashX[i]-radius,flashY[i]-radius*.22f,flashX[i]+radius,flashY[i]+radius*.22f,paint);
            canvas.drawRect(flashX[i]-radius*.22f,flashY[i]-radius,flashX[i]+radius*.22f,flashY[i]+radius,paint);
            paint.setColor(0xfffff0b6);paint.setAlpha((int)(240*(1-progress)));
            canvas.drawRect(flashX[i]-radius*.3f,flashY[i]-radius*.3f,flashX[i]+radius*.3f,flashY[i]+radius*.3f,paint);
        }
        for(int i=0;i<LIMIT;i++) if(life[i]>0) {
            int alpha=(int)(255*Math.min(1,life[i]/.32f));
            float left=Math.round(x[i]),top=Math.round(y[i]);
            float edge=Math.max(1,w[i]*.12f);
            paint.setColor(0xff362e2c);paint.setAlpha(alpha);
            canvas.drawRect(left-edge,top-edge,left+w[i]+edge,top+h[i]+edge,paint);
            paint.setColor(colors[i]);paint.setAlpha(alpha);
            canvas.drawRect(left,top,left+w[i],top+h[i],paint);
            paint.setColor(0xffd7bd94);paint.setAlpha(alpha);
            canvas.drawRect(left,top,left+w[i],top+Math.max(1,h[i]*.22f),paint);
        }
        paint.setStyle(originalStyle);paint.setColor(originalColor);paint.setAlpha(originalAlpha);
    }
}

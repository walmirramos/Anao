package com.flappybird.recreation;
import android.graphics.*;
/** Fixed-size coin splash. Timers advance only during active gameplay. */
final class FactoryCelebration {
    private final float[] x=new float[48],y=new float[48],vx=new float[48],vy=new float[48],life=new float[48];
    private final Paint p=new Paint();
    private int next;private String label="";private float labelX,labelY,labelLife;
    void clear(){java.util.Arrays.fill(life,0);labelLife=0;next=0;}
    void splash(float cx,float cy,float scale,int amount){
        label="+"+amount+" COINS";labelX=cx;labelY=cy-25*scale;labelLife=.9f;
        for(int k=0;k<9;k++){
            int i=next++%48;double a=Math.PI*(1.12+k*.095);
            x[i]=cx;y[i]=cy;vx[i]=(float)Math.cos(a)*70*scale;vy[i]=(float)Math.sin(a)*100*scale;
            life[i]=.8f;
        }
    }
    void tick(float dt){
        for(int i=0;i<48;i++)if(life[i]>0){life[i]-=dt;x[i]+=vx[i]*dt;y[i]+=vy[i]*dt;vy[i]+=180*dt;}
        labelLife=Math.max(0,labelLife-dt);labelY-=20*dt;
    }
    void draw(Canvas c,float s){
        for(int i=0;i<48;i++)if(life[i]>0){
            p.setColor(0xff8a5516);p.setAlpha((int)(255*Math.min(1,life[i]*4)));
            c.drawRect(x[i]-3*s,y[i]-4*s,x[i]+3*s,y[i]+4*s,p);
            p.setColor(0xffffc743);p.setAlpha((int)(255*Math.min(1,life[i]*4)));
            c.drawRect(x[i]-2*s,y[i]-3*s,x[i]+2*s,y[i]+3*s,p);
            p.setColor(0xffffefa5);c.drawRect(x[i]-s,y[i]-2*s,x[i],y[i]+s,p);
        }
        if(labelLife>0){p.setTypeface(PixelFont.current());p.setTextAlign(Paint.Align.CENTER);p.setTextSize(12*s);
            p.setColor(0xff12232e);c.drawText(label,labelX+s,labelY+s,p);
            p.setColor(0xffffdf6b);c.drawText(label,labelX,labelY,p);}
        p.setAlpha(255);
    }
}

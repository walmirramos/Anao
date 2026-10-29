package com.flappybird.recreation;
/** Gentle paired motion: fixed gap, constrained edges, frozen near the player. */
final class SafePipeMotion {
    private float offset,phase;
    float tick(int score,float seconds,float min,float max,boolean crossing,float scale) {
        if(score<30){offset=0;phase=0;return 0;}
        if(!(seconds>0) || !Float.isFinite(seconds) || crossing || min>max)return offset;
        float dt=Math.min(seconds,.05f);phase=(phase+dt*1.05f)%6.2831853f;
        float amplitude=14*Math.max(.1f,scale);
        float target=Math.max(min,Math.min(max,(float)Math.sin(phase)*amplitude));
        float step=12*Math.max(.1f,scale)*dt;
        offset+=Math.max(-step,Math.min(step,target-offset));
        offset=Math.max(min,Math.min(max,offset));return offset;
    }
    void reset(){offset=0;phase=0;}
}

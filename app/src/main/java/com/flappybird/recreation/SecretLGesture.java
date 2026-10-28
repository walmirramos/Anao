package com.flappybird.recreation;

/** Single finger: down, then right. Evaluate the whole stroke so a rounded corner is valid. */
final class SecretLGesture {
    private static final int CAPACITY=128;
    private final float[] xs=new float[CAPACITY],ys=new float[CAPACITY];
    private float minimumLeg, width, height, maxDistance;
    private long startedAt,lastTime;
    private int count;
    private boolean tracking,valid;

    void begin(float x, float y, long time, float width, float height) {
        this.width=width;this.height=height;
        minimumLeg=Math.max(32f,Math.min(120f,Math.min(width,height)*.10f));
        startedAt=lastTime=time;maxDistance=0;count=1;xs[0]=x;ys[0]=y;
        tracking=true;
        valid=width>0 && height>0 && finite(width) && finite(height) && inside(x,y);
    }

    void move(float x, float y, long time) {
        if(!tracking || !valid) return;
        if(time-startedAt>4000 || time<lastTime || !inside(x,y)) { valid=false;return; }
        lastTime=time;
        maxDistance=Math.max(maxDistance,Math.max(Math.abs(x-xs[0]),Math.abs(y-ys[0])));
        // Keep spatial samples rather than every high-rate MotionEvent. Always retain the latest point.
        if(count>1 && Math.abs(x-xs[count-2])+Math.abs(y-ys[count-2])<Math.max(2,minimumLeg*.04f)) {
            xs[count-1]=x;ys[count-1]=y;return;
        }
        if(count==CAPACITY) {
            for(int i=0;i<CAPACITY/2;i++) { xs[i]=xs[i*2];ys[i]=ys[i*2]; }
            count=CAPACITY/2;
        }
        xs[count]=x;ys[count]=y;count++;
    }

    boolean end(float x, float y, long time) {
        move(x,y,time);
        boolean recognized=tracking && valid && time-startedAt>=80 && time-startedAt<=4000 && matches();
        cancel();return recognized;
    }

    void cancel() { tracking=false;valid=false; }
    /** Remains available after release/cancel until begin(), so drags cannot trigger a menu button. */
    boolean isStroke() { return maxDistance>=Math.max(12f,minimumLeg*.25f); }

    private boolean matches() {
        if(count<3) return false;
        float startX=xs[0],startY=ys[0],endX=xs[count-1],endY=ys[count-1];
        float backtrack=Math.max(6f,minimumLeg*.22f);
        for(int corner=1;corner<count-1;corner++) {
            float down=ys[corner]-startY,right=endX-xs[corner];
            float leanX=xs[corner]-startX,leanY=endY-ys[corner];
            if(down<minimumLeg || right<minimumLeg || Math.abs(leanX)>down*.4f || Math.abs(leanY)>right*.4f) continue;
            float deviation=Math.max(8f,Math.min(down,right)*.28f);
            float farY=startY,farX=xs[corner];
            boolean shape=true;
            for(int i=1;i<=corner;i++) {
                float expectedX=startX+(ys[i]-startY)*leanX/down;
                if(Math.abs(xs[i]-expectedX)>deviation || ys[i]<farY-backtrack || ys[i]<startY-backtrack) {shape=false;break;}
                farY=Math.max(farY,ys[i]);
            }
            if(!shape) continue;
            for(int i=corner+1;i<count;i++) {
                float expectedY=ys[corner]+(xs[i]-xs[corner])*leanY/right;
                if(Math.abs(ys[i]-expectedY)>deviation || xs[i]<farX-backtrack || xs[i]<xs[corner]-backtrack) {shape=false;break;}
                farX=Math.max(farX,xs[i]);
            }
            if(shape) return true;
        }
        return false;
    }
    private static boolean finite(float value) { return !Float.isNaN(value) && !Float.isInfinite(value); }
    private boolean inside(float x,float y) { return finite(x) && finite(y) && x>=0 && y>=0 && x<=width && y<=height; }
}

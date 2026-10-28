package com.flappybird.recreation;

import android.view.MotionEvent;

/** Observes one continuous finger stroke without swallowing normal control events. */
final class SecretGestureTouch {
    private final SecretLGesture gesture=new SecretLGesture();
    private int pointerId=-1;
    private boolean following;

    /** Returns true only for the release that completes a recognized L. */
    boolean onTouchEvent(MotionEvent event, float width, float height) {
        int action=event.getActionMasked();
        if(event.getPointerCount()!=1 || action==MotionEvent.ACTION_CANCEL ||
                action==MotionEvent.ACTION_POINTER_DOWN || action==MotionEvent.ACTION_POINTER_UP) {
            cancel();return false;
        }
        if(action==MotionEvent.ACTION_DOWN) {
            cancel();pointerId=event.getPointerId(0);following=true;
            gesture.begin(event.getX(0),event.getY(0),event.getEventTime(),width,height);
            return false;
        }
        if(!following) return false;
        int index=event.findPointerIndex(pointerId);
        if(index<0) { cancel();return false; }
        if(action!=MotionEvent.ACTION_MOVE && action!=MotionEvent.ACTION_UP) return false;
        for(int h=0;h<event.getHistorySize();h++) {
            gesture.move(event.getHistoricalX(index,h),event.getHistoricalY(index,h),event.getHistoricalEventTime(h));
        }
        if(action==MotionEvent.ACTION_UP) {
            boolean recognized=gesture.end(event.getX(index),event.getY(index),event.getEventTime());
            following=false;pointerId=-1;
            return recognized;
        }
        gesture.move(event.getX(index),event.getY(index),event.getEventTime());
        return false;
    }

    void cancel() { gesture.cancel();following=false;pointerId=-1; }
}

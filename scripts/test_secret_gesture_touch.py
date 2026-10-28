"""Event-routing checks for the real touch adapter, with a minimal MotionEvent fake."""
from pathlib import Path
import os
import subprocess
import tempfile

MOTION=r'''package android.view;
public class MotionEvent {
 public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,
  ACTION_POINTER_DOWN=5,ACTION_POINTER_UP=6;
 public int action,pointerId,pointers=1;public long time;public float x,y;
 public float[] hx=new float[0],hy=new float[0];public long[] ht=new long[0];
 public MotionEvent(int a,long t,float x,float y,int id){action=a;time=t;this.x=x;this.y=y;pointerId=id;}
 public int getActionMasked(){return action;}public int getPointerCount(){return pointers;}
 public int getPointerId(int i){if(i!=0)throw new AssertionError("unexpected pointer");return pointerId;}
 public int findPointerIndex(int id){return id==pointerId?0:-1;}
 public float getX(int i){if(i!=0)throw new AssertionError("unexpected pointer");return x;}
 public float getY(int i){if(i!=0)throw new AssertionError("unexpected pointer");return y;}
 public long getEventTime(){return time;}public int getHistorySize(){return ht.length;}
 public float getHistoricalX(int i,int h){if(i!=0)throw new AssertionError("unexpected pointer");return hx[h];}
 public float getHistoricalY(int i,int h){if(i!=0)throw new AssertionError("unexpected pointer");return hy[h];}
 public long getHistoricalEventTime(int h){return ht[h];}
}'''

TEST=r'''package com.flappybird.recreation;
import android.view.MotionEvent;
class SecretGestureTouchTest {
 static int checks;
 static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static MotionEvent e(int action,long t,float x,float y){return new MotionEvent(action,t,x,y,7);}
 static boolean send(SecretGestureTouch a,MotionEvent e){return a.onTouchEvent(e,400,800);}
 static void down(SecretGestureTouch a){check(!send(a,e(0,1000,70,80)),"down is never consumed");}
 static boolean finish(SecretGestureTouch a){send(a,e(2,1150,70,210));send(a,e(2,1300,130,210));return send(a,e(1,1500,230,210));}
 public static void main(String[] args){
  SecretGestureTouch a=new SecretGestureTouch();
  check(!send(a,e(1,1500,230,210)),"release without down cannot recognize");
  down(a);check(!send(a,e(2,1150,70,210)),"vertical leg is only observed");
  check(!send(a,e(2,1300,130,210)),"corner is only observed");
  check(send(a,e(1,1500,230,210)),"only the completing release is consumed");
  check(!send(a,e(1,1550,230,210)),"duplicate release cannot toggle again");

  down(a);MotionEvent batched=e(1,1500,230,210);
  batched.hx=new float[]{70,70,130};batched.hy=new float[]{140,210,210};batched.ht=new long[]{1080,1150,1300};
  check(send(a,batched),"batched historical corner survives sparse events");
  down(a);MotionEvent move=e(2,1400,170,210);
  move.hx=new float[]{70,130};move.hy=new float[]{210,210};move.ht=new long[]{1150,1300};
  check(!send(a,move),"batched move does not consume control events");
  check(send(a,e(1,1500,230,210)),"batched move history is used before release");

  down(a);a.cancel();check(!finish(a),"explicit cancel stops the whole stroke");
  down(a);check(!send(a,e(3,1100,70,150)),"ACTION_CANCEL is never consumed");
  check(!finish(a),"canceled stream cannot resume without a fresh down");
  down(a);MotionEvent multi=e(5,1100,70,150);multi.pointers=2;
  check(!send(a,multi),"second finger cancels tracking");check(!finish(a),"multitouch release cannot toggle");
  down(a);check(!send(a,e(6,1100,70,150)),"pointer-up action cancels even malformed one-pointer stream");
  check(!finish(a),"pointer-up stream cannot resume");
  down(a);MotionEvent replaced=e(2,1150,70,210);replaced.pointerId=12;
  check(!send(a,replaced),"missing original pointer cancels");check(!finish(a),"pointer replacement cannot complete L");
  down(a);check(finish(a),"fresh down works after any cancellation");
  down(a);check(!send(a,e(1,1100,71,82)),"normal tap remains available to controls");
  down(a);send(a,e(2,1200,70,200));check(!send(a,e(1,1500,70,300)),"normal scroll does not toggle");
  down(a);send(a,e(2,1200,170,180));check(!send(a,e(1,1500,270,280)),"diagonal scroll does not toggle");
  MotionEvent invalid=e(0,1000,70,80);invalid.pointers=2;
  check(!send(a,invalid)&&!finish(a),"multi-pointer down never starts a secret stroke");
  check(!SecretInvincibility.isEnabled(),"session begins with invincibility disabled");
  check(SecretInvincibility.toggle()&&SecretInvincibility.isEnabled(),"shared toggle activates session");
  check(!SecretInvincibility.toggle()&&!SecretInvincibility.isEnabled(),"shared toggle deactivates session");
  System.out.println("Secret touch routing: "+checks+" event and shared-state checks passed");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    java=root/'app/src/main/java/com/flappybird/recreation'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-secret-touch-') as folder:
        tmp=Path(folder)
        motion=tmp/'android/view/MotionEvent.java';motion.parent.mkdir(parents=True)
        motion.write_text(MOTION,encoding='utf-8')
        test=tmp/'SecretGestureTouchTest.java';test.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(motion),str(test),
            str(java/'SecretGestureTouch.java'),str(java/'SecretLGesture.java'),str(java/'SecretInvincibility.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.SecretGestureTouchTest'],check=True)

if __name__=='__main__':main()

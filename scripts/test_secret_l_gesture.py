"""JVM checks for the real secret gesture recognizer; no Android runtime required."""
from pathlib import Path
import os, subprocess, tempfile

TEST='''package com.flappybird.recreation;
class SecretLGestureTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static SecretLGesture start(){SecretLGesture g=new SecretLGesture();g.begin(70,80,1000,400,800);return g;}
 static boolean trace(float width,float height,long duration,float... p){
  SecretLGesture g=new SecretLGesture();int points=p.length/2;g.begin(p[0],p[1],1000,width,height);
  for(int i=1;i<points-1;i++)g.move(p[2*i],p[2*i+1],1000+duration*i/(points-1));
  return g.end(p[p.length-2],p[p.length-1],1000+duration);
 }
 public static void main(String[] args){
  SecretLGesture g=start();g.move(73,120,1080);g.move(72,180,1190);g.move(105,184,1280);
  check(g.end(190,181,1420),"down then right with natural drift");
  check(g.isStroke(),"drag remains observable after successful release");
  check(trace(400,800,500,70,80,70,200,240,200),"three sampled points are sufficient");
  check(trace(400,800,500,70,80,72,140,78,185,92,204,125,211,200,214),"rounded finger corner");
  check(trace(400,800,500,70,80,66,140,69,188,79,203,123,208,204,203),"rounded corner with leftward drift");
  check(trace(400,800,500,70,80,72,125,70,122,75,180,110,182,107,180,190,182),"small hand tremor");
  check(trace(400,800,80,70,80,70,180,220,180),"fast deliberate 80 ms gesture");
  check(trace(400,800,100,70,80,70,180,220,180),"100 ms gesture formerly rejected");
  check(trace(400,800,3800,70,80,70,180,220,180),"slow deliberate gesture");
  check(trace(400,800,4000,70,80,70,180,220,180),"four second upper duration boundary");
  check(trace(1080,2400,600,200,400,204,550,215,590,252,609,350,611,430,614),"rounded phone stroke in physical pixels");
  check(trace(1200,2400,500,210,240,210,390,360,390),"large screen uses a shorter practical stroke");
  check(trace(3000,4000,500,210,240,210,380,350,380),"tablet minimum leg is capped at 120 pixels");
  check(trace(200,400,500,30,30,30,65,65,65),"small screen 32 pixel minimum");
  check(!trace(200,400,500,30,30,30,60,60,60),"small screen still requires deliberate legs");
  g=start();check(!g.end(71,81,1100),"ordinary tap");
  check(!g.isStroke(),"tap does not count as a drag");
  g=start();g.move(70,180,1200);check(!g.end(70,260,1450),"vertical swipe");
  check(g.isStroke(),"unrecognized swipe is still an observable drag");
  g=start();g.move(150,80,1200);check(!g.end(240,80,1450),"horizontal swipe");
  g=start();g.move(150,180,1200);check(!g.end(240,260,1450),"diagonal");
  check(!trace(400,800,500,70,80,220,230),"one unsampled diagonal cannot prove a corner");
  check(!trace(400,800,500,70,80,120,130,180,190,240,250),"densely sampled diagonal");
  check(!trace(400,800,500,70,80,90,140,120,200,150,260),"steep diagonal");
  check(!trace(400,800,500,70,80,140,100,210,130,280,150),"shallow diagonal");
  g=start();g.move(70,180,1200);g.move(90,180,1300);check(!g.end(105,180,1450),"right leg too short");
  g=start();g.move(70,110,1100);g.move(110,110,1200);check(!g.end(220,110,1400),"down leg too short");
  g=start();g.move(70,180,1200);g.move(40,180,1300);check(!g.end(0,180,1450),"leftward L");
  check(!trace(400,800,500,70,240,70,120,220,120),"up then right is not L");
  g=start();g.move(150,80,1200);g.move(150,180,1300);check(!g.end(150,240,1450),"right then down");
  g=start();g.move(70,180,1200);g.move(110,180,1300);check(!g.end(220,245,1450),"hook drifts down too far");
  g=start();g.move(70,180,1020);g.move(110,180,1040);check(!g.end(220,180,1060),"under 80 ms is too fast");
  g=start();g.move(70,180,2200);g.move(110,180,4100);check(!g.end(220,180,5100),"over four seconds is too slow");
  g=start();g.move(70,180,1200);g.cancel();check(!g.end(220,180,1450),"canceled/multitouch stroke");
  g=start();g.move(70,180,1200);g.move(110,180,1300);g.move(160,180,1400);g.move(100,180,1500);
  check(!g.end(240,180,1600),"large backtracking");
  check(!trace(400,800,500,70,80,70,200,70,135,70,220,220,220),"vertical backtracking");
  check(!trace(400,800,500,70,80,130,140,10,170,70,200,240,200),"strong vertical zigzag");
  check(!trace(400,800,500,70,80,70,180,125,240,180,120,250,180),"strong horizontal zigzag");
  check(!trace(400,800,500,70,80,70,180,180,180,180,80,300,80),"U-shaped detour");
  g=start();g.move(70,180,1200);g.move(110,180,1300);check(!g.end(450,180,1450),"outside the view");
  g=new SecretLGesture();g.begin(210,240,1000,1200,2400);g.move(210,540,1200);g.move(330,540,1300);
  check(g.end(660,540,1500),"same gesture scaled for a larger screen");
  g=start();g.move(70,180,1200);g.move(110,180,1190);check(!g.end(220,180,1450),"timestamps must stay monotonic");
  check(!trace(0,800,500,70,80,70,180,220,180),"zero view width");
  check(!trace(Float.NaN,800,500,70,80,70,180,220,180),"invalid view dimensions");
  check(!trace(400,800,500,70,80,Float.NaN,180,220,180),"invalid finger coordinates");
  check(!trace(400,800,500,70,80,70,180,Float.POSITIVE_INFINITY,180),"infinite release coordinate");
  g=start();g.move(70,180,1200);g.move(110,180,1300);check(g.end(220,180,1500),"fresh valid stroke");
  check(!g.end(220,180,1600),"same release cannot toggle twice");
  g.begin(70,80,2000,400,800);check(!g.isStroke(),"fresh stroke clears drag flag");
  g=start();for(int i=1;i<=2000;i++){
   float p=i/2000f;g.move(p<=.5f?70:70+(p-.5f)*300,p<=.5f?80+p*300:230,1000+i);
  }
  check(g.end(220,230,3100),"dense high-refresh input survives bounded-buffer compaction");
  g=start();for(int i=1;i<=2000;i++)g.move(70+i*.075f,80+i*.075f,1000+i);
  check(!g.end(220,230,3100),"dense diagonal survives compaction without false recognition");
  System.out.println("Secret L gesture: "+checks+" positive and negative checks passed");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-gesture-') as folder:
        tmp=Path(folder);src=tmp/'SecretLGestureTest.java';src.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),str(root/'app/src/main/java/com/flappybird/recreation/SecretLGesture.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.SecretLGestureTest'],check=True)

if __name__=='__main__':main()

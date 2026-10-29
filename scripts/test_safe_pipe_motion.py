from pathlib import Path
import os,subprocess,tempfile
r=Path.cwd();jdk=Path(os.environ.get('JAVA_HOME','C:/Program Files/Android/openjdk/jdk-21.0.8'))/'bin'
s='''package com.flappybird.recreation;
class MotionTest {
 static int checks;
 static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 public static void main(String[] args){
  for(float scale:new float[]{.5f,1,2.5f}){
   SafePipeMotion m=new SafePipeMotion();
   check(m.tick(29,.016f,-30*scale,30*scale,false,scale)==0,"stationary before 30");
   float last=0;
   for(int i=0;i<2000;i++){
    float v=m.tick(30,.016f,-30*scale,30*scale,false,scale);
    check(Math.abs(v)<=14*scale+.001f,"bounded amplitude");
    check(Math.abs(v-last)<=12*scale*.016f+.001f,"bounded vertical speed");last=v;
   }
   check(m.tick(40,1,-30*scale,30*scale,true,scale)==last,"freeze near crossing");
   check(m.tick(40,Float.NaN,-30*scale,30*scale,false,scale)==last,"invalid time ignored");
   check(m.tick(40,0,-30*scale,30*scale,false,scale)==last,"pause freezes motion");
   m.reset();check(m.tick(29,.016f,-1,1,false,scale)==0,"reset");
   for(int i=0;i<300;i++)check(Math.abs(m.tick(30,.016f,-2*scale,2*scale,false,scale))<=2*scale+.001,"edge margin");
  }
  System.out.println("Safe pipe motion: "+checks+" checks passed");
 }
}'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'MotionTest.java';p.write_text(s)
 subprocess.run([str(jdk/'javac.exe'),'-d',d,str(p),str(r/'app/src/main/java/com/flappybird/recreation/SafePipeMotion.java')],check=True)
 subprocess.run([str(jdk/'java.exe'),'-cp',d,'com.flappybird.recreation.MotionTest'],check=True)

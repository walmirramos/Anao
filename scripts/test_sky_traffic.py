"""Scheduler checks against the real SkyTraffic class, with minimal drawing API fakes."""
from pathlib import Path
import os
import subprocess
import tempfile

STUBS = {
    'android/graphics/Canvas.java': '''package android.graphics;
public class Canvas {
 public int paths,rectangles;
 public void save(){} public void restore(){} public void translate(float x,float y){} public void scale(float x,float y){}
 public void drawPath(Path p,Paint a){paths++;}
 public void drawRect(float a,float b,float c,float d,Paint p){rectangles++;}
}''',
    'android/graphics/Paint.java': '''package android.graphics;
public class Paint {
 public enum Style{FILL}
 public int color=0xff123456;
 public void setAntiAlias(boolean value){} public void setStyle(Style style){}
 public int getAlpha(){return color>>>24;} public void setColor(int value){color=value;}
}''',
    'android/graphics/Path.java': '''package android.graphics;
public class Path {public void moveTo(float x,float y){} public void lineTo(float x,float y){} public void close(){}}''',
    'com/flappybird/recreation/SkyTrafficTest.java': '''package com.flappybird.recreation;
import java.lang.reflect.Field;import java.util.Random;import android.graphics.*;
class SkyTrafficTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 static Object field(SkyTraffic t,String name)throws Exception{Field f=SkyTraffic.class.getDeclaredField(name);f.setAccessible(true);return f.get(t);}
 static float number(SkyTraffic t,String name)throws Exception{return ((Number)field(t,name)).floatValue();}
 static boolean visible(SkyTraffic t)throws Exception{return (Boolean)field(t,"visible");}
 public static void main(String[] args)throws Exception{
  SkyTraffic t=new SkyTraffic(new Random(19));Canvas c=new Canvas();Paint p=new Paint();
  float wait=number(t,"untilNext");check(wait>=18&&wait<=28&&!visible(t),"initial rare-aircraft delay");
  for(int i=0;i<100;i++)t.draw(c,p);
  check(c.paths==0&&number(t,"untilNext")==wait,"draw calls never advance time or spawn planes");
  t.tick(wait-.02f,false);check(!visible(t),"does not spawn early");
  t.tick(.03f,false);check(visible(t)&&number(t,"direction")==-1,"one plane begins moving left");
  float start=number(t,"x");t.tick(1,true);
  check(number(t,"x")<start&&number(t,"direction")==-1,"reversing settings does not turn an active flight");
  check(number(t,"y")>=52&&number(t,"y")<127,"flight remains in high sky");
  int before=p.color;t.draw(c,p);
  check(c.paths==1&&p.color==before,"one plane drawn without mutating caller paint");
  t.tick(20,false);check(!visible(t),"plane exits screen");
  wait=number(t,"untilNext");check(wait>=35&&wait<=60,"long interval only begins after exit");
  t.tick(wait+.01f,true);check(visible(t)&&number(t,"direction")==1,"next reverse flight travels right");
  t.reset();check(!visible(t)&&number(t,"untilNext")>=18&&number(t,"untilNext")<=28,"new run clears old flight");
  wait=number(t,"untilNext");t.tick(-1,false);t.tick(Float.NaN,false);t.tick(Float.POSITIVE_INFINITY,false);
  check(number(t,"untilNext")==wait,"invalid elapsed times are ignored");
  System.out.println("SkyTraffic: 12 scheduling/drawing checks passed (isolated drawing fakes)");
 }
}''',
}

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-sky-') as folder:
        tmp=Path(folder)
        for name,data in STUBS.items():
            p=tmp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data,encoding='utf-8')
        sources=[str(p) for p in tmp.rglob('*.java')]
        sources.append(str(root/'app/src/main/java/com/flappybird/recreation/SkyTraffic.java'))
        subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(tmp),*sources],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.SkyTrafficTest'],check=True)

if __name__=='__main__':main()

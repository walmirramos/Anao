"""Pure JVM checks for the actual collision mapper used by the game."""
from pathlib import Path
import os, subprocess, tempfile

TEST = r'''package com.flappybird.recreation;
import java.util.Arrays;
class WallCollisionTest {
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 static boolean hit(boolean[] mask,float l,float t,float r,float b,boolean flipped){
  return WallCollision.intersects(mask,l,t,r,b,100,200,104,640,flipped);
 }
 public static void main(String[] args){
  boolean[] mask=new boolean[52*320];Arrays.fill(mask,true);
  check(hit(mask,104,204,112,212,false),"opaque wall");
  check(!hit(mask,60,204,100,212,false),"touching left edge is not overlap");
  check(!hit(mask,205,204,210,212,false),"outside width");
  check(!hit(mask,104,150,112,200,false),"touching top edge is not overlap");
  check(!hit(mask,104,841,112,850,false),"outside height");
  for(int y=0;y<8;y++)for(int x=0;x<11;x++)mask[y*52+x]=false;
  check(!hit(mask,102,202,118,214,false),"broken notch is traversable at double scale");
  check(hit(mask,118,202,128,214,false),"partly overlapping intact brick");
  check(hit(mask,102,218,118,230,false),"brick below notch");
  check(!hit(mask,102,826,118,838,true),"flipped notch at bottom of top wall");
  check(hit(mask,102,202,118,214,true),"flipped wall retains opposite end");
  check(!WallCollision.intersects(mask,10,10,20,20,0,0,0,10,false),"zero width");
  Arrays.fill(mask,false);
  check(!hit(mask,100,200,204,840,false),"transparent wall never collides");
  System.out.println("Wall collision: 12 checks passed (native alpha mapping)");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-wall-') as folder:
        tmp=Path(folder);src=tmp/'WallCollisionTest.java';src.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),
            str(root/'app/src/main/java/com/flappybird/recreation/WallCollision.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.WallCollisionTest'],check=True)

if __name__=='__main__':main()

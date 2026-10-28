"""Pure JVM checks for actual-hitbox bazooka targeting, including reverse play."""
from pathlib import Path
import os
import subprocess
import tempfile

TEST='''package com.flappybird.recreation;
class BazookaTargetingTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static boolean hit(float actorLeft,float actorRight,float wallLeft,float wallRight,boolean reverse,float margin){
  return BazookaTargeting.imminent(actorLeft,actorRight,wallLeft,wallRight,reverse,margin);
 }
 public static void main(String[] args){
  // Ordinary 34-pixel actor: decide from its leading edge, not its center.
  check(BazookaTargeting.ahead(100,134,150,202,false),"default wall ahead");
  check(!hit(100,134,150,202,false,8),"default wall outside safety margin");
  check(hit(100,134,142,194,false,8),"default wall at exact margin");
  check(hit(100,134,139,191,false,8),"default wall within margin");

  check(BazookaTargeting.ahead(100,134,40,92,true),"reverse wall ahead");
  check(!hit(100,134,32,84,true,8),"reverse wall outside safety margin");
  check(hit(100,134,40,92,true,8),"reverse wall at exact margin");
  check(hit(100,134,45,97,true,8),"reverse wall within margin");

  // Custom large and small hitboxes must not behave like a fixed radius.
  check(hit(60,180,185,237,false,8),"large hitbox uses actual right edge");
  check(!hit(103,137,185,237,false,8),"same center with normal hitbox is not imminent");
  check(hit(60,180,3,55,true,8),"large reverse hitbox uses actual left edge");
  check(!hit(103,137,3,55,true,8),"same center normal reverse hitbox remains distant");
  check(!hit(100,104,124,176,false,8),"small hitbox does not inherit fixed seventeen-pixel radius");
  check(!hit(100,104,80,84,true,8),"small reverse hitbox does not inherit fixed radius");
  check(hit(100,200,200,252,false,0),"very large actor detects front contact without margin");
  check(hit(100,200,48,100,true,0),"very large reverse actor detects front contact");

  // Actual wall bounds matter even if the wall is far wider than the original sprite.
  check(hit(100,134,150,450,false,16),"wide wall uses actual left edge");
  check(hit(100,134,-200,84,true,16),"wide reverse wall uses actual right edge");
  check(hit(100,134,40,160,false,0),"wide wall overlapping actor remains targetable");
  check(hit(100,134,80,220,true,0),"wide reverse overlap remains targetable");

  check(!BazookaTargeting.ahead(100,134,20,99,false),"fully behind forward wall excluded");
  check(!hit(100,134,20,99,false,10000),"huge margin cannot select a wall behind actor");
  check(!BazookaTargeting.ahead(100,134,135,190,true),"fully behind reverse wall excluded");
  check(!hit(100,134,135,190,true,10000),"huge reverse margin cannot select behind wall");
  check(!hit(100,134,48,100,false,8),"trailing-edge-only contact is already behind");
  check(!hit(100,134,134,186,true,8),"reverse trailing-edge-only contact is behind");

  check(hit(100,134,120,172,false,0),"current overlap triggers without lookahead");
  check(hit(100,134,62,114,true,0),"current reverse overlap triggers without lookahead");
  check(hit(100,134,50,210,false,0),"actor entirely inside wall triggers forward");
  check(hit(100,134,50,210,true,0),"actor entirely inside wall triggers reverse");

  check(!hit(100,134,Math.nextUp(142f),200,false,8),"one float above forward margin stays distant");
  check(hit(100,134,Math.nextDown(142f),200,false,8),"one float below forward margin triggers");
  check(!hit(100,134,40,Math.nextDown(92f),true,8),"one float beyond reverse margin stays distant");
  check(hit(100,134,40,Math.nextUp(92f),true,8),"one float inside reverse margin triggers");
  check(hit(100,134,134,186,false,0),"forward leading-edge touch triggers");
  check(hit(100,134,48,100,true,0),"reverse leading-edge touch triggers");

  check(!hit(100,134,135,187,false,-20),"negative margin does not grant positive forward lookahead");
  check(!hit(100,134,47,99,true,-20),"negative margin does not grant reverse lookahead");
  check(hit(100,134,134,186,false,-20),"negative margin clamps to zero at forward contact");
  check(hit(100,134,48,100,true,-20),"negative margin clamps to zero at reverse contact");
  check(hit(100,134,130,182,false,-20),"negative margin still detects existing overlap");

  // Translation, mirroring and scale keep the geometric decision unchanged.
  check(hit(10100,10134,10142,10194,false,8),"translation does not introduce fixed world origin");
  check(hit(-134,-100,-194,-142,true,8),"mirrored scene keeps the same decision");
  check(hit(300,402,426,582,false,24),"scaled actor wall and margin preserve contact");
  check(!hit(300,402,450,606,false,24),"scaled gap outside margin stays distant");
  System.out.println("BazookaTargeting: "+checks+" actual-hitbox geometry checks passed");
 }
}'''


def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-bazooka-targeting-') as folder:
        tmp=Path(folder)
        src=tmp/'BazookaTargetingTest.java'
        src.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),
            str(root/'app/src/main/java/com/flappybird/recreation/BazookaTargeting.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),
            'com.flappybird.recreation.BazookaTargetingTest'],check=True)


if __name__=='__main__':main()

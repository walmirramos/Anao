"""Test the actual collectible/timer state with no Android runtime dependency."""
from pathlib import Path
import os,subprocess,tempfile

TEST='''package com.flappybird.recreation;
class BananaBonusTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args){
  BananaBonus b=new BananaBonus();
  check(!b.shouldSpawn(19)&&b.shouldSpawn(20),"first banana unlocks at 20");
  b.spawned(20,15);check(b.visible()&&!b.shouldSpawn(99),"one visible banana");
  check(b.collect()&&b.active()&&b.remaining()==10,"collection starts ten seconds");
  b.tick(4);check(!b.collect()&&b.remaining()==6,"duplicate collection never refreshes timer");
  check(!b.shouldSpawn(99),"no overlapping power ups");
  b.tick(-1);check(b.remaining()==6,"negative time cannot extend aura");
  b.tick(5.5f);check(b.active()&&b.remaining()==.5f,"aura stays active before ten seconds");
  b.tick(.5f);check(!b.active()&&b.remaining()==0,"aura expires exactly at ten seconds");
  check(!b.shouldSpawn(34)&&b.shouldSpawn(35),"later randomized score threshold");
  b.spawned(35,12);b.miss();check(!b.visible()&&!b.active()&&!b.shouldSpawn(46),"miss does not grant immunity");
  check(b.shouldSpawn(47),"miss waits until the next spawn");
  b.spawned(47,10);b.collect();b.end();check(!b.active()&&!b.visible(),"death clears aura");
  b.reset();check(b.shouldSpawn(20)&&!b.shouldSpawn(19),"new run restores first bonus");
  b.tick(100);check(b.remaining()==0,"timer never goes negative");
  System.out.println("Banana bonus: 14 state and timer checks passed");
 }
}'''

def main():
 root=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
 with tempfile.TemporaryDirectory(prefix='corre-nengue-banana-') as folder:
  tmp=Path(folder);src=tmp/'BananaBonusTest.java';src.write_text(TEST,encoding='utf-8')
  subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),str(root/'app/src/main/java/com/flappybird/recreation/BananaBonus.java')],check=True)
  subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.BananaBonusTest'],check=True)

if __name__=='__main__':main()

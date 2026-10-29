"""Pure JVM checks for the actual three-kind collectible state and active timer."""
from pathlib import Path
import os
import subprocess
import tempfile

TEST='''package com.flappybird.recreation;
import java.util.Random;
import java.util.EnumSet;
class PowerUpStateTest {
 static int checks;
 static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
 static void expire(PowerUpState p){
  if(p.active(PowerUpState.Kind.BAZUCA)){
   while(p.rounds()>0){check(p.fireShot(),"round available");p.resolveShot();}
  } else p.tick(10);
 }
 public static void main(String[] args){
  PowerUpState p=new PowerUpState(new Random(42));
  check(!p.visible()&&!p.active()&&p.activeKind()==null&&p.collectibleKind()==null,"initial state");
  check(!p.shouldSpawn(2)&&p.shouldSpawn(3),"first factory bonus unlocks at three");
  p.spawned(2,10);check(!p.visible(),"premature spawn cannot advance sequence");
  p.spawned(20,15);check(p.collectibleKind()==PowerUpState.Kind.SAYAJIN,"first appearance is aura");
  p.spawned(999,10);check(p.collectibleKind()==PowerUpState.Kind.SAYAJIN,"duplicate spawn ignored");
  check(!p.shouldSpawn(999)&&p.collect(),"one collectible can be collected");
  check(p.active(PowerUpState.Kind.SAYAJIN)&&!p.active(PowerUpState.Kind.CELULAR)&&p.remaining()==10,"typed ten-second effect");
  check(p.collectibleKind()==null&&!p.visible(),"collected item disappears");
  p.tick(4);check(!p.collect()&&p.remaining()==6,"duplicate collection does not refresh");
  p.spawned(999,10);check(!p.visible()&&!p.shouldSpawn(999),"no stacking or spawning during effect");
  p.tick(-2);p.tick(Float.NaN);check(p.remaining()==6,"invalid time cannot extend or corrupt timer");
  p.tick(5.5f);check(p.active()&&p.remaining()==.5f,"active before exact expiry");
  p.tick(.5f);check(!p.active()&&p.activeKind()==null&&p.remaining()==0,"exact ten-second expiry");
  check(!p.shouldSpawn(34)&&p.shouldSpawn(35),"caller-provided later score gap");
  p.spawned(35,12);check(p.collectibleKind()==PowerUpState.Kind.CELULAR,"second appearance is phone");
  p.miss();check(!p.visible()&&!p.active()&&p.collectibleKind()==null,"miss grants no effect");
  check(!p.shouldSpawn(46)&&p.shouldSpawn(47),"miss waits until next threshold");
  p.spawned(47,10);check(p.collectibleKind()==PowerUpState.Kind.BAZUCA,"third appearance is bazooka after missed phone");
  p.collect();check(p.active(PowerUpState.Kind.BAZUCA),"bazooka effect is typed");
  check(p.rounds()==3&&p.remaining()==0,"bazooka gives three rounds without timer");
  p.tick(9999);check(p.active()&&p.rounds()==3,"ammo never expires");
  for(int i=3;i>0;i--){
   check(p.fireShot()&&p.rounds()==i-1,"launch consumes exactly one round");
   check(!p.fireShot(),"no second launch while rocket is in flight");
   p.tick(9999);check(p.active(),"last rocket remains active until impact");
   p.resolveShot();
  }
  check(!p.active()&&!p.fireShot()&&p.rounds()==0,"cannot fire fourth shot");
  PowerUpState.Kind previous=PowerUpState.Kind.BAZUCA;
  EnumSet<PowerUpState.Kind> seen=EnumSet.noneOf(PowerUpState.Kind.class);
  int score=57;
  for(int i=0;i<200;i++){
   check(p.shouldSpawn(score),"later eligible spawn");
   p.spawned(score,10);PowerUpState.Kind next=p.collectibleKind();
   check(next!=null&&next!=previous,"random appearances never repeat immediately");
   seen.add(next);p.collect();check(p.active(next),"random kind preserved through collection");
   expire(p);previous=next;score+=10;
  }
  check(seen.size()==3,"random phase can contain all kinds");
  p.reset();check(p.shouldSpawn(3)&&!p.shouldSpawn(2),"reset restores first threshold");
  p.spawned(20,10);check(p.collectibleKind()==PowerUpState.Kind.SAYAJIN,"reset restores guaranteed sequence");
  p.collect();p.end();check(!p.active()&&!p.visible()&&p.activeKind()==null,"end clears active and collectible state");
  check(!p.shouldSpawn(29)&&p.shouldSpawn(30),"end does not restart score scheduling");
  p.spawned(30,10);p.end();check(p.collectibleKind()==null&&!p.collect(),"end removes uncollected item");
  PowerUpState a=new PowerUpState(new Random(987)),b=new PowerUpState(new Random(987));
  for(int i=0;i<40;i++){
   int points=20+i*10;a.spawned(points,10);b.spawned(points,10);
   check(a.collectibleKind()==b.collectibleKind(),"injected seeded randomness is deterministic");
   a.miss();b.miss();
  }
  check(new PowerUpState().shouldSpawn(3),"default constructor");
  PowerUpState early=new PowerUpState(new Random(1));early.spawned(3,4);early.miss();
  check(!early.shouldSpawn(6)&&early.shouldSpawn(7),"factory four-point schedule");
  System.out.println("Power-up state: "+checks+" checks passed, including 200 random transitions");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-powerups-') as folder:
        tmp=Path(folder);src=tmp/'PowerUpStateTest.java';src.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),
            str(root/'app/src/main/java/com/flappybird/recreation/PowerUpState.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.PowerUpStateTest'],check=True)

if __name__=='__main__':main()

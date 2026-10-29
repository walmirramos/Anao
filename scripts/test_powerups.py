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
 static void clear(PowerUpState p){
  if(p.active(PowerUpState.Kind.BAZUCA))while(p.rounds()>0){check(p.fireShot(),"round available");p.resolveShot();}
  p.tick(10);p.consumeShield();
 }
 public static void main(String[] args){
  PowerUpState p=new PowerUpState(new Random(42));
  check(!p.active()&&!p.visible()&&p.activeKind()==null,"initial state");
  check(!p.shouldSpawn(2)&&p.shouldSpawn(3),"first unlock");
  p.spawned(2,4);check(!p.visible(),"early spawn ignored");
  p.spawned(3,4);check(p.collectibleKind()==PowerUpState.Kind.SAYAJIN&&p.collect(),"tire grants shield");
  check(p.active(PowerUpState.Kind.SAYAJIN)&&p.remaining()==0,"shield has no timer");
  p.tick(999999);p.tick(Float.POSITIVE_INFINITY);p.tick(Float.NaN);
  check(p.active(PowerUpState.Kind.SAYAJIN),"shield never expires");
  check(!p.collect(),"no phantom second charge");
  check(!p.shouldSpawn(6)&&p.shouldSpawn(7),"shield permits other bonus spawns");
  p.spawned(7,4);check(p.collectibleKind()==PowerUpState.Kind.CELULAR&&p.collect(),"drill collected alongside shield");
  check(p.active(PowerUpState.Kind.SAYAJIN)&&p.active(PowerUpState.Kind.CELULAR),"independent shield and drill");
  check(p.remaining()==10&&!p.shouldSpawn(99),"drill retains ten-second nonstacking timer");
  p.tick(-2);p.tick(Float.NaN);check(p.remaining()==10,"invalid time ignored");
  p.tick(4);check(p.remaining()==6&&p.active(PowerUpState.Kind.SAYAJIN),"drill timer cannot expire shield");
  check(p.consumeShield()&&!p.active(PowerUpState.Kind.SAYAJIN),"first wall consumes shield");
  check(!p.consumeShield()&&p.active(PowerUpState.Kind.CELULAR)&&p.remaining()==6,"no second life; drill unaffected");
  p.tick(6);check(!p.active(),"drill expires normally");
  p.spawned(11,4);check(p.collectibleKind()==PowerUpState.Kind.BAZUCA&&p.collect(),"third appearance bazooka");
  p.tick(9999);check(p.active(PowerUpState.Kind.BAZUCA)&&p.rounds()==3,"ammo has no expiry");
  for(int i=3;i>0;i--){
   check(p.fireShot()&&p.rounds()==i-1,"exactly one round per shot");
   check(!p.fireShot(),"one rocket in flight");p.tick(9999);check(p.active(),"last rocket allowed to finish");p.resolveShot();
  }
  check(!p.active()&&!p.fireShot(),"no fourth shot");
  PowerUpState q=new PowerUpState(new Random(9));
  q.spawned(3,4);q.collect();q.spawned(7,4);q.miss();q.spawned(11,4);q.collect();
  check(q.active(PowerUpState.Kind.SAYAJIN)&&q.active(PowerUpState.Kind.BAZUCA),"shield coexists with bazooka");
  q.tick(99999);check(q.consumeShield()&&q.rounds()==3,"shield hit preserves ammo");
  q.end();check(!q.active()&&!q.visible()&&q.rounds()==0,"death clears inventories");
  p.reset();EnumSet<PowerUpState.Kind> seen=EnumSet.noneOf(PowerUpState.Kind.class);PowerUpState.Kind previous=null;
  for(int i=0;i<200;i++){
   int score=3+i*4;check(p.shouldSpawn(score),"next item eligible");p.spawned(score,4);
   PowerUpState.Kind kind=p.collectibleKind();check(kind!=previous,"no repeat appearances");seen.add(kind);
   check(p.collect()&&p.active(kind),"collected kind activated");clear(p);check(!p.active(),"charges and timers clear independently");previous=kind;
  }
  check(seen.size()==3,"all bonuses covered");
  p.reset();p.spawned(3,4);p.collect();p.reset();check(!p.active()&&!p.consumeShield()&&p.shouldSpawn(3),"restart clears extra life");
  System.out.println("Power-up state: "+checks+" checks passed, including persistent extra life, coexistence and three-shot ammo");
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

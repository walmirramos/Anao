"""Pure JVM geometry and lifecycle checks for the actual WallStatus class."""
from pathlib import Path
import os
import subprocess
import tempfile

TEST='''package com.flappybird.recreation;
class WallStatusTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void near(float actual,float expected,String name){check(Math.abs(actual-expected)<.0001f,name+": "+actual+" vs "+expected);}
 public static void main(String[] args){
  WallStatus grace=new WallStatus();grace.reset();
  check(!grace.hasShieldPass(),"fresh wall has no shield grace");
  grace.grantShieldPass();check(grace.hasShieldPass()&&!grace.isDestroyed(),"absorbed wall stays visible with contact grace");
  grace.reset();check(!grace.hasShieldPass(),"recycled wall is dangerous again");

  WallStatus w=new WallStatus();
  check(!w.isDestroyed()&&w.generation()==0,"fresh wall lifecycle");
  near(w.extra(),0,"fresh opening");near(w.top(120),120,"fresh top");near(w.bottom(260),260,"fresh bottom");
  w.updateOpening(true,100,0,false);
  near(w.extra(),100,"phone opens immediately without elapsed time");
  near(w.top(120),70,"top recedes by half extra");near(w.bottom(260),310,"bottom recedes by half extra");
  near((w.top(120)+w.bottom(260))/2,190,"gap center stays fixed");
  near(w.bottom(260)-w.top(120),240,"rendered opening gains exactly the supplied extra");
  w.updateOpening(true,100,.6f,true);near(w.extra(),100,"active phone does not accumulate opening");
  w.updateOpening(false,100,.175f,false);near(w.extra(),50,"halfway through closing");
  near(w.top(120),95,"closing top shares current geometry");near(w.bottom(260),285,"closing bottom shares current geometry");
  w.updateOpening(false,100,.175f,false);near(w.extra(),0,"fully closes after 0.35 seconds");
  w.updateOpening(false,100,10,false);near(w.extra(),0,"oversized close never becomes negative");

  w.updateOpening(true,80,0,false);
  w.updateOpening(false,80,.7f,true);near(w.extra(),80,"expiration holds while actor crosses");
  w.updateOpening(false,80,100,true);near(w.extra(),80,"long crossing still cannot close on actor");
  w.updateOpening(false,80,.175f,false);near(w.extra(),40,"closing begins after actor clears");
  w.updateOpening(false,80,.175f,false);near(w.extra(),0,"held opening later closes completely");
  w.updateOpening(true,90,0,false);w.updateOpening(false,90,.1f,false);
  float held=w.extra();w.updateOpening(false,90,2,true);
  near(w.extra(),held,"actor entering during partial close freezes shared geometry");
  w.updateOpening(true,90,0,true);near(w.extra(),90,"reactivation reopens immediately");

  w.updateOpening(false,90,-2,false);near(w.extra(),90,"negative elapsed time cannot increase opening");
  w.updateOpening(true,-50,0,false);near(w.extra(),0,"negative requested extra clamps to zero");
  near(w.top(120),120,"negative extra cannot invert top");near(w.bottom(260),260,"negative extra cannot invert bottom");
  w.updateOpening(true,60,0,false);w.updateOpening(false,-60,.2f,false);
  near(w.extra(),60,"negative close rate cannot enlarge opening");

  check(w.destroy(),"first destruction succeeds");check(w.isDestroyed(),"destroyed flag is observable");
  check(!w.destroy(),"duplicate destruction emits no second event");
  int generation=w.generation();
  w.updateOpening(true,75,.2f,false);w.updateOpening(false,75,2,false);
  check(w.isDestroyed(),"opening updates do not resurrect destroyed wall");
  check(w.generation()==generation,"opening and destruction do not recycle generation");
  w.reset();check(!w.isDestroyed(),"recycling restores intact wall");
  near(w.extra(),0,"recycling clears phone opening");
  check(w.generation()==generation+1,"recycling increments generation once");
  check(w.destroy()&&!w.destroy(),"new generation can be destroyed exactly once");
  w.reset();check(w.generation()==generation+2,"later recycle increments again");
  near(w.top(-300),-300,"reset preserves negative world coordinates");
  near(w.bottom(-150),-150,"reset bottom preserves negative world coordinates");
  System.out.println("WallStatus: "+checks+" geometry and lifecycle checks passed");
 }
}'''

def main():
    root=Path(__file__).resolve().parents[1]
    jdk=Path(os.environ['JAVA_HOME'])/'bin'
    with tempfile.TemporaryDirectory(prefix='corre-nengue-wall-status-') as folder:
        tmp=Path(folder);src=tmp/'WallStatusTest.java';src.write_text(TEST,encoding='utf-8')
        subprocess.run([str(jdk/'javac.exe'),'-d',str(tmp),str(src),
            str(root/'app/src/main/java/com/flappybird/recreation/WallStatus.java')],check=True)
        subprocess.run([str(jdk/'java.exe'),'-cp',str(tmp),'com.flappybird.recreation.WallStatusTest'],check=True)

if __name__=='__main__':main()

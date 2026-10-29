from pathlib import Path
import os,subprocess,tempfile
r=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
source=(r/'app/src/main/java/com/flappybird/recreation/MainActivity.java').read_text(encoding='utf-8')
pipe='package com.flappybird.recreation; import java.util.Random; import android.graphics.*;\n'+source[source.index('class Pipe {'):]
stubs={
'android/graphics/Paint.java':'package android.graphics;public class Paint{public void setFilterBitmap(boolean b){}public void setAntiAlias(boolean b){}public void setColorFilter(ColorFilter f){}}',
'android/graphics/ColorFilter.java':'package android.graphics;public class ColorFilter{}',
'android/graphics/Rect.java':'package android.graphics;public class Rect{public void set(int a,int b,int c,int d){}}',
'com/flappybird/recreation/WorldArt.java':'package com.flappybird.recreation;class WorldArt{static final int WALL_STYLES=6;}',
'com/flappybird/recreation/Pipe.java':pipe,
'com/flappybird/recreation/FixedOpeningTest.java':"""package com.flappybird.recreation;
class FixedOpeningTest{
 public static void main(String[] args){
  Pipe p=new Pipe(120,52,320);p.resetHeight(120,400,0,1);float top=p.getTopPipeY(),bottom=p.getBottomPipeY(120);boolean moved=false;
  for(int i=0;i<1000;i++){
   p.updateFactoryMotion(30,.016f,0,400,120,false,1);
   if(p.textureOffset()!=0)moved=true;
   if(p.getTopPipeY()!=top||p.getBottomPipeY(120)!=bottom)throw new AssertionError("opening must remain fixed");
  }
  if(!moved)throw new AssertionError("pipe texture must move");
  p.status.updateOpening(true,40,0,false);float expandedTop=p.status.top(p.getTopPipeY()),expandedBottom=p.status.bottom(p.getBottomPipeY(120));
  for(int i=0;i<100;i++){
   p.updateFactoryMotion(40,.016f,0,400,120,false,1);
   if(p.status.top(p.getTopPipeY())!=expandedTop||p.status.bottom(p.getBottomPipeY(120))!=expandedBottom)throw new AssertionError("drill opening is stationary too");
  }
  System.out.println("Fixed opening: native pipe coordinates and drill geometry preserved across 1100 animation frames");
 }
}"""}
with tempfile.TemporaryDirectory() as d:
 tmp=Path(d)
 for name,text in stubs.items():
  p=tmp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
 files=[str(p) for p in tmp.rglob('*.java')]+[str(r/'app/src/main/java/com/flappybird/recreation'/n) for n in ['SafePipeMotion.java','WallStatus.java']]
 subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',d,*files],check=True)
 subprocess.run([str(jdk/'java.exe'),'-cp',d,'com.flappybird.recreation.FixedOpeningTest'],check=True)

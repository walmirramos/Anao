from pathlib import Path
import os,subprocess,tempfile
r=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
s=(r/'app/src/main/java/com/flappybird/recreation/MainActivity.java').read_text(encoding='utf-8');a=s.index('    private ColorFilter createPipeColorFilter(');b=s.index('    private void update(',a)
method=s[a:b].replace('private ColorFilter','static ColorFilter')
stubs={
'android/graphics/ColorFilter.java':'package android.graphics;public class ColorFilter{}',
'android/graphics/PorterDuff.java':'package android.graphics;public class PorterDuff{public enum Mode{MULTIPLY,SRC_ATOP}}',
'android/graphics/PorterDuffColorFilter.java':'package android.graphics;public class PorterDuffColorFilter extends ColorFilter{public PorterDuffColorFilter(int c,PorterDuff.Mode m){}}',
'android/graphics/ColorMatrixColorFilter.java':'package android.graphics;public class ColorMatrixColorFilter extends ColorFilter{public final float[] m;public ColorMatrixColorFilter(float[] a){m=a;}}',
'FilterTest.java':'''import android.graphics.*;
class FilterTest {'''+method+'''
 static int map(float[] m,int r,int g,int b,int alpha,int row){int i=row*5;return Math.min(255,Math.max(0,Math.round(m[i]*r+m[i+1]*g+m[i+2]*b+m[i+3]*alpha+m[i+4])));}
 public static void main(String[] args){
  ColorFilter f=createPipeColorFilter(4);
  if(!(f instanceof ColorMatrixColorFilter))throw new AssertionError("white must preserve luminance, not cover texture");
  float[] m=((ColorMatrixColorFilter)f).m;
  int dark=map(m,21,43,56,255,0),mid=map(m,89,119,132,255,0),light=map(m,186,205,210,255,0);
  if(!(dark<mid&&mid<light&&light<255))throw new AssertionError("steel contrast lost");
  for(int a:new int[]{0,32,128,255})if(map(m,89,119,132,a,3)!=a)throw new AssertionError("alpha changed");
  for(int i=1;i<=8;i++)if(createPipeColorFilter(i)==null)throw new AssertionError("palette missing");
  if(createPipeColorFilter(0)!=null)throw new AssertionError("default art should be untouched");
  System.out.println("Pipe colors: white steel retains contrast ("+dark+", "+mid+", "+light+") and alpha; all palette entries valid");
 }
}'''}
with tempfile.TemporaryDirectory() as d:
 for name,text in stubs.items():
  p=Path(d)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
 subprocess.run([str(jdk/'javac.exe'),'-d',d,*[str(p) for p in Path(d).rglob('*.java')]],check=True)
 subprocess.run([str(jdk/'java.exe'),'-cp',d,'FilterTest'],check=True)

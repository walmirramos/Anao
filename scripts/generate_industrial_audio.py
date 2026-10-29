"""Original automotive workshop sound design. Requires NumPy; writes PCM WAV."""
from pathlib import Path
import wave,numpy as np
root=Path(__file__).resolve().parents[1];raw=root/'app/src/main/res/raw';sr=22050
rng=np.random.default_rng(1102026)
def time(d):return np.arange(round(d*sr))/sr
def write(name,a):
 peak=np.max(np.abs(a))
 if peak>.88:a=a*(.88/peak)
 with wave.open(str(raw/(name+'.wav')),'wb') as w:
  w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((a*32767).astype('<i2').tobytes())
def air(d):
 n=rng.normal(0,.35,round(d*sr));return n-np.convolve(n,np.ones(13)/13,'same')
def clang(d=.5,f=180):
 t=time(d);return sum(np.sin(2*np.pi*f*k*t)*np.exp(-t*(5+i*3))/(i+2) for i,k in enumerate([1,1.43,2.17,2.91,4.13]))*.5+air(d)*np.exp(-t*60)*.13
def servo(d=.65):
 t=time(d);f=210+480*np.sin(np.pi*t/d)**2;phase=np.cumsum(f)*2*np.pi/sr
 env=np.sin(np.pi*t/d)**.6
 return (np.sin(phase)+.3*np.sin(2*phase)+.1*np.sin(5*phase))*env*.13+air(d)*env*.035
def place(a,b,start):
 idx=(np.arange(len(b))+round(start*sr))%len(a);np.add.at(a,idx,b)
t=time(12)
# Continuous electrical motors, ventilation and conveyor bearings, not siren tones.
motors=.085*np.sin(2*np.pi*55*t)+.035*np.sin(2*np.pi*110*t)+.018*np.sin(2*np.pi*165*t)
motors+=(.025*np.sin(2*np.pi*29*t)+.009*np.sin(2*np.pi*460*t))*(.8+.2*np.sin(2*np.pi*t/3))
vent=rng.normal(0,.12,len(t));vent=np.convolve(vent,np.ones(27)/27,'same');motors+=vent
for start in np.arange(0,12,.75):place(motors,clang(.18,125)*.45,start)
for start in [1.1,4.1,7.1,10.1]:
 place(motors,servo(.8),start)
 u=time(.38);place(motors,air(.38)*np.sin(np.pi*u/.38)**1.2*.14,start+.8)
for start in [2.4,5.4,8.4,11.4]:place(motors,clang(.3,340)*.32,start)
write('police_8bit',motors)
# Restrained 100 BPM workshop groove: bass, damped metal and ratchet percussion.
music=np.zeros(len(t));notes=[55,55,65.406,49,55,73.416,65.406,49,55,55,82.407,73.416,65.406,55,49,41.203,55,65.406,73.416,49]
for beat,f in enumerate(notes):
 u=time(.6);env=np.minimum(u/.012,1)*np.minimum((.6-u)/.07,1)
 place(music,(np.sin(2*np.pi*f*u)+.2*np.sin(4*np.pi*f*u))*env*.16,beat*.6)
 place(music,clang(.12,260)*.27,beat*.6+.3)
 if beat%2:place(music,clang(.25,95)*.28,beat*.6)
write('nengue_chiptune',music)
write('swooshing',servo(.42))
u=time(.14);write('wing',air(.14)*np.sin(np.pi*u/.14)**1.5*.16+.08*np.sin(2*np.pi*140*u)*np.exp(-u*30))
u=time(.34);write('point',(.14*np.sin(2*np.pi*1046.5*u)+.08*np.sin(2*np.pi*1568*u))*np.exp(-u*14))
write('hit',clang(.6,125));write('falling_burst',clang(.8,85)*.6)
u=time(.9);blast=air(.9)*np.exp(-u*8)*.6+.38*np.sin(2*np.pi*(65*u-22*u*u))*np.exp(-u*5)
blast+=clang(.9,74)*.25;write('wall_shot',blast)
write('thunder',blast*.8);write('rain',motors*.7);write('storm_backtrack',motors*.8)
write('aura_activate',servo(.65))
u=time(1);write('aura_loop',.075*np.sin(2*np.pi*80*u)+.025*np.sin(2*np.pi*160*u))
# Original comic sad-trombone fail sting, descending wah-wah (no sampled meme).
fail=np.zeros(round(1.7*sr))
for start,d,f in [(0,.27,311.127),(.29,.27,293.665),(.58,.27,277.183),(.88,.8,261.626)]:
 u=time(d);fall=(u/d)**2*38 if d>.5 else u*3
 phase=np.cumsum(f-fall)*2*np.pi/sr;env=np.minimum(u/.025,1)*np.minimum((d-u)/.08,1)
 brass=sum(np.sin(phase*k)*(1+.3*np.sin(2*np.pi*5*u))/(k**1.25) for k in range(1,9))
 place(fail,brass*env*.19,start)
write('die',fail)
print('Workshop motors, pneumatic hiss, servo tools, conveyor, explosion and original comic fail rendered')

from pathlib import Path
import numpy as np, wave, shutil, json
root=Path(__file__).resolve().parents[1];raw=root/'app/src/main/res/raw';sr=22050
rng=np.random.default_rng(2026)
def save(name,a):
 a=np.nan_to_num(a);a=np.clip(a,-.92,.92)
 for p in raw.glob(name+'.*'):p.unlink()
 with wave.open(str(raw/(name+'.wav')),'wb') as w:
  w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((a*32767).astype('<i2').tobytes())
def timeline(d):return np.arange(round(d*sr))/sr
def metal(d=.55,f=220):
 t=timeline(d);a=sum(np.sin(2*np.pi*f*k*t)*np.exp(-t*(8+i*3))/(i+2) for i,k in enumerate([1,1.47,2.09,3.71]))
 a+=rng.uniform(-1,1,len(t))*.12*np.exp(-t*45)
 return a*.5
# Seamless four-bar industrial chiptune, 120 BPM, original composition.
t=timeline(8);music=np.zeros(len(t));notes=[82.41,82.41,110,98,82.41,123.47,110,73.42,82.41,98,110,146.83,123.47,110,98,73.42]
for beat,f in enumerate(notes):
 i=round(beat*.5*sr);n=round(.5*sr);u=np.arange(n)/sr
 env=np.minimum(u/.008,1)*np.minimum((.5-u)/.04,1)
 bass=(np.sin(2*np.pi*f*u)+.25*np.sin(4*np.pi*f*u))*env*.12
 music[i:i+n]+=bass
 music[i:i+n]+=.18*np.sin(2*np.pi*(48*u+35*(1-np.exp(-u*25))/25))*np.exp(-u*18)
 for offset in [0,.25]:
  j=i+round(offset*sr);hit=metal(.12,700 if offset else 310)*.18;music[j:j+len(hit)]+=hit
 if beat%2:
  hit=metal(.22,360)*.25;music[i:i+len(hit)]+=hit
save('nengue_chiptune',music)
# Ventilation + motor harmonics + conveyor cycle; no emergency/police siren.
machine=.06*np.sin(2*np.pi*55*t)+.035*np.sin(2*np.pi*110*t)+.018*np.sin(2*np.pi*220*t)*(1+.4*np.sin(2*np.pi*.5*t))
for i in range(16):
 hit=metal(.25,140)*.14;j=round(i*.5*sr);machine[j:j+len(hit)]+=hit
save('police_8bit',machine)
for name,d,f in [('hit',.45,170),('wall_shot',.7,85),('falling_burst',.65,120),('thunder',.8,60)]:save(name,metal(d,f))
for name,d,f,end in [('wing',.1,420,640),('point',.22,660,990),('die',.55,260,80),('swooshing',.16,180,480),('aura_activate',.55,220,880)]:
 u=timeline(d);env=np.minimum(u/.008,1)*np.minimum((d-u)/.04,1);phase=2*np.pi*(f*u+(end-f)*u*u/(2*d));save(name,.2*np.sin(phase)*env)
u=timeline(1);save('aura_loop',.09*(np.sin(2*np.pi*110*u)+.4*np.sin(2*np.pi*220*u))*(.7+.3*np.sin(2*np.pi*4*u)))
save('rain',machine*.6);save('storm_backtrack',machine*.8+music*.3)

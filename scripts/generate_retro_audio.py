"""Original 128 BPM chiptune + arcade police siren. Requires numpy; writes PCM WAV."""
from pathlib import Path
import wave
import numpy as np

RATE = 22050
BEAT = 60 / 128
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'

def tone(freq, length, kind='pulse'):
    t = np.arange(round(length * RATE)) / RATE
    phase = (freq * t) % 1
    if kind == 'triangle':
        sound = 1 - 4 * np.abs(phase - .5)
    else:
        sound = np.where(phase < .25, 1., -1.) + .5
    envelope = np.minimum(t / .006, 1) * np.minimum((length - t) / .035, 1)
    return sound * envelope

def write(name, data):
    assert np.max(np.abs(data)) <= 1
    pcm = np.round(data * 32767).astype('<i2')
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / name), 'wb') as f:
        f.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        f.writeframes(pcm.tobytes())
    print(name, f'{len(data)/RATE:.2f}s', 'peak', round(float(np.max(np.abs(data))), 3))

def main():
    music = np.zeros(round(32 * BEAT * RATE))
    def add(sound, start, gain):
        pos = round(start * RATE)
        count = min(len(sound), len(music) - pos)
        music[pos:pos+count] += gain * sound[:count]
    def hz(midi): return 440 * 2 ** ((midi - 69) / 12)
    melody = [76,79,81,79,76,72,74,76, 74,77,79,77,74,71,72,74,
              72,76,79,84,83,79,76,74, 71,74,79,77,74,71,67,71] * 2
    for i,note in enumerate(melody): add(tone(hz(note), BEAT*.42), i*BEAT/2, .14)
    roots = [48,45,53,43,48,45,53,43]
    for beat in range(32):
        add(tone(hz(roots[beat//4]), BEAT*.8, 'triangle'), beat*BEAT, .2)
        t=np.arange(round(.13*RATE))/RATE
        phase=np.cumsum(125*np.exp(-t*25)+45)/RATE
        add(np.sin(2*np.pi*phase)*np.exp(-t*28), beat*BEAT, .22)
    rng=np.random.default_rng(42)
    for half in range(64):
        t=np.arange(round(.055*RATE))/RATE
        noise=rng.uniform(-1,1,len(t))
        add(noise*np.exp(-t*85), half*BEAT/2, .055)
        if half%4==2:
            add(noise*np.exp(-t*38), half*BEAT/2, .1)
    # Identical zero endpoints avoid clicks when the loop wraps.
    music[:100] *= np.linspace(0,1,100)
    music[-100:] *= np.linspace(1,0,100)
    write('nengue_chiptune.wav', music)
    # Smooth electronic wail, fast yelp and two-tone tail; lightly quantized for retro character.
    t=np.arange(8*RATE)/RATE
    wail=700+730*(.5-.5*np.cos(2*np.pi*t/4))
    yelp=750+650*(.5-.5*np.cos(2*np.pi*(t-4)/.28))
    hilo=np.where((np.floor((t-6)/.32).astype(int)%2)==0,790.,1070.)
    freq=np.where(t<4,wail,np.where(t<6,yelp,hilo))
    phase=2*np.pi*np.cumsum(freq)/RATE
    siren=(np.sin(phase)+.30*np.sin(3*phase)+.12*np.sin(5*phase)+.05*np.sin(7*phase))*.24
    siren=np.round(siren*96)/96
    siren*=.9+.1*np.sin(2*np.pi*t*7)
    siren[:450]*=np.linspace(0,1,450)
    siren[-650:]*=np.linspace(1,0,650)
    write('police_8bit.wav',siren)
    # A percussive shot: noise crack, low body and a short reflected tail, without laser glissando.
    t=np.arange(round(.26*RATE))/RATE
    noise=np.random.default_rng(2027).uniform(-1,1,len(t))
    high=noise-.85*np.roll(noise,1)
    low=np.convolve(noise,np.ones(9)/9,mode='same')
    shot=.58*high*np.exp(-t*95)+.34*np.sin(2*np.pi*145*t)*np.exp(-t*32)
    shot+=.25*low*np.exp(-t*20)
    delay=round(.055*RATE)
    shot[delay:]+=.16*shot[:-delay].copy()
    shot=np.round(shot*96)/96
    shot*=.85/max(.85,float(np.max(np.abs(shot))))
    shot[:15]*=np.linspace(0,1,15)
    shot[-150:]*=np.linspace(1,0,150)
    write('wall_shot.wav',shot)
    burst=np.zeros(round(1.1*RATE))
    for start in [0,.18,.36]:
        pos=round(start*RATE)
        count=min(len(shot),len(burst)-pos)
        burst[pos:pos+count]+=shot[:count]
    write('falling_burst.wav',burst)
    t=np.arange(round(4*BEAT*RATE))/RATE
    arpeggio=np.array([72,76,79,84,79,76,74,79])
    notes=arpeggio[(np.floor(t/(BEAT/2)).astype(int))%len(arpeggio)]
    freq=440*2**((notes-69)/12)
    phase=np.cumsum(freq)/RATE
    aura=.12*np.where(phase%1<.25,1.,-1.)
    aura+=.075*np.sin(2*np.pi*130.81*t)+.055*np.sin(2*np.pi*196*t)
    aura+=np.random.default_rng(10).uniform(-1,1,len(t))*.025
    aura[:120]*=np.linspace(0,1,120);aura[-120:]*=np.linspace(1,0,120)
    write('aura_loop.wav',aura)
    charge=np.zeros(round(.55*RATE))
    for i,note in enumerate([72,76,79,84]):
        sound=tone(hz(note),.18)*.22
        start=round(i*.10*RATE);count=min(len(sound),len(charge)-start)
        charge[start:start+count]+=sound[:count]
    charge[-100:]*=np.linspace(1,0,100)
    write('aura_activate.wav',charge)

if __name__ == '__main__': main()

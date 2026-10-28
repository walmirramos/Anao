'use strict';

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
const revealNodes = document.querySelectorAll('.reveal');
if ('IntersectionObserver' in window && !reducedMotion.matches) {
  document.documentElement.classList.add('motion-ready');
  const reveals = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        reveals.unobserve(entry.target);
      }
    });
  }, { threshold: 0.08 });
  revealNodes.forEach(node => reveals.observe(node));
}

const powers = {
  sayajin: {
    kicker: 'BANANA → SAYAJIN', title: 'O brilho<br> é outro.',
    description: 'Pegou a banana? A aura dourada entra em cena. São 10 segundos de proteção contra as paredes, com efeito sonoro próprio.',
    note: 'A primeira banana aparece a partir de 20 Nencoins. O chão continua valendo.',
    alt: 'Demonstração ilustrativa do poder Sayajin com aura dourada.'
  },
  phone: {
    kicker: 'CELULAR → PASSAGEM AMPLIADA', title: 'Mais espaço.<br> Mais respiro.',
    description: 'Coletou o celular? O vão entre as paredes fica maior por 10 segundos. Um pouco de folga para continuar no corre.',
    note: 'A abertura aumenta em até 50%. Ao acabar, espera o personagem passar antes de fechar.',
    alt: 'Demonstração ilustrativa do celular ampliando o vão entre duas paredes.'
  },
  bazooka: {
    kicker: 'BAZUCA → CAMINHO LIVRE', title: 'O obstáculo?<br> Virou história.',
    description: 'Deixe a bazuca fazer o trabalho pesado. Durante 10 segundos, ela dispara automaticamente e explode as paredes à frente.',
    note: 'Flash, tijolos caindo e espaço para seguir. O chão continua sendo um obstáculo.',
    alt: 'Demonstração ilustrativa da bazuca, com foguete e tijolos explodindo.'
  }
};
let activePower = 'sayajin';
let powerStartedAt = performance.now();
let activeColor = 0;
let assets;
let redrawStatic = () => {};
const powerTabs = Array.from(document.querySelectorAll('[data-power][role="tab"]'));
function selectPower(key) {
  if (!powers[key]) return;
  activePower = key;
  powerStartedAt = performance.now();
  const power = powers[key];
  powerTabs.forEach(tab => {
    const selected = tab.dataset.power === key;
    tab.classList.toggle('is-active', selected);
    tab.setAttribute('aria-selected', String(selected));
    tab.tabIndex = selected ? 0 : -1;
  });
  document.querySelector('#power-panel').dataset.power = key;
  document.querySelector('#power-panel').setAttribute('aria-labelledby', `tab-${key}`);
  document.querySelector('#power-kicker').textContent = power.kicker;
  document.querySelector('#power-title').innerHTML = power.title;
  document.querySelector('#power-description').textContent = power.description;
  document.querySelector('#power-note').textContent = power.note;
  document.querySelector('#power-game').setAttribute('aria-label', power.alt);
  redrawStatic();
}
powerTabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectPower(tab.dataset.power));
  tab.addEventListener('keydown', event => {
    let next = index;
    if (event.key === 'ArrowRight') next = (index + 1) % powerTabs.length;
    else if (event.key === 'ArrowLeft') next = (index + powerTabs.length - 1) % powerTabs.length;
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = powerTabs.length - 1;
    else return;
    event.preventDefault();
    powerTabs[next].focus();
    selectPower(powerTabs[next].dataset.power);
  });
});

const colorNames = ['Original. O clássico da correria.', 'Azul. Sangue-frio no corre.', 'Rosa. Presença em qualquer fase.'];
document.querySelectorAll('[data-color]').forEach(button => {
  button.addEventListener('click', () => {
    activeColor = Number(button.dataset.color);
    document.querySelectorAll('[data-color]').forEach(option => {
      const selected = Number(option.dataset.color) === activeColor;
      option.classList.toggle('is-active', selected);
      option.setAttribute('aria-pressed', String(selected));
    });
    document.querySelector('#color-name').textContent = colorNames[activeColor];
    document.querySelector('#character-art').setAttribute('aria-label', `Protagonista em pixel art na cor ${['original', 'azul', 'rosa'][activeColor]}, com óculos escuros e barba.`);
    document.querySelector('.character-glow').style.background = `radial-gradient(ellipse,${['#f5e5d6', '#dce8fb', '#f3ddec'][activeColor]} 0,#fafafc00 65%)`;
    drawCharacter();
  });
});

const music = new Audio('assets/music.wav');
const siren = new Audio('assets/siren.wav');
music.preload = siren.preload = 'none';
music.loop = siren.loop = true;
music.volume = 0.23;
siren.volume = 0.1;
const soundButton = document.querySelector('#sound-toggle');
let soundPlaying = false;
let soundRequest = 0;
function stopSound() {
  soundRequest++;
  music.pause(); siren.pause();
  soundPlaying = false;
  soundButton.setAttribute('aria-pressed', 'false');
  document.querySelector('#sound-label').textContent = 'Ouvir o clima';
}
soundButton.addEventListener('click', async () => {
  if (soundPlaying) { stopSound(); return; }
  const request = ++soundRequest;
  soundButton.disabled = true;
  try {
    await Promise.all([music.play(), siren.play()]);
    if (request !== soundRequest || document.hidden) { music.pause(); siren.pause(); return; }
    soundPlaying = true;
    soundButton.setAttribute('aria-pressed', 'true');
    document.querySelector('#sound-label').textContent = 'Pausar o som';
    document.querySelector('.sound-status').textContent = 'Música e sirene em reprodução.';
  } catch {
    stopSound();
    document.querySelector('.sound-status').textContent = 'Não foi possível reproduzir o som. Tente novamente.';
  } finally { soundButton.disabled = false; }
});
document.addEventListener('visibilitychange', () => {
  if (document.hidden) stopSound();
  else requestDraw();
});
window.addEventListener('pagehide', stopSound);

function imageAt(url) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(`Image could not load: ${url}`));
    image.src = url;
  });
}
function canvasOf(width, height) {
  const canvas = document.createElement('canvas');
  canvas.width = width; canvas.height = height;
  return canvas;
}
function spritesFrom(image) {
  // Same transparent bounds and channel permutations used by CharacterSprites in the APK.
  const source = canvasOf(image.width, image.height);
  const context = source.getContext('2d', { willReadFrequently: true });
  context.drawImage(image, 0, 0);
  const rgba = context.getImageData(0, 0, image.width, image.height).data;
  let left = image.width, right = -1, top = image.height, bottom = -1;
  for (let y = 0; y < image.height; y++) {
    for (let x = 0; x < image.width; x++) {
      if (rgba[(y * image.width + x) * 4 + 3] > 128) {
        left = Math.min(left, x); right = Math.max(right, x);
        top = Math.min(top, y); bottom = Math.max(bottom, y);
      }
    }
  }
  if (right < left) throw new Error('Empty character image');
  const width = 34, height = Math.round(width * (bottom - top + 1) / (right - left + 1));
  return [0, 1, 2].map(variant => {
    const sprite = canvasOf(width, height);
    const ctx = sprite.getContext('2d');
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(source, left, top, right - left + 1, bottom - top + 1, 0, 0, width, height);
    if (variant) {
      const pixels = ctx.getImageData(0, 0, width, height);
      for (let i = 0; i < pixels.data.length; i += 4) {
        const r = pixels.data[i], g = pixels.data[i + 1], b = pixels.data[i + 2];
        pixels.data[i] = variant === 1 ? b : r;
        pixels.data[i + 1] = variant === 1 ? g : b;
        pixels.data[i + 2] = variant === 1 ? r : g;
      }
      ctx.putImageData(pixels, 0, 0);
    }
    return sprite;
  });
}

function wall(ctx, x, y, width, height, faded = false) {
  if (height <= 0) return;
  ctx.save();
  ctx.beginPath(); ctx.rect(x, y, width, height); ctx.clip();
  ctx.globalAlpha = faded ? 0.7 : 1;
  ctx.fillStyle = '#453c3b'; ctx.fillRect(x, y, width, height);
  const brickWidth = width / 2.15, brickHeight = width / 6.5;
  const colors = ['#b95335', '#d9784b', '#74392e', '#bd7151'];
  for (let row = 0; row < Math.ceil(height / brickHeight); row++) {
    for (let column = -1; column < 3; column++) {
      const bx = x + column * brickWidth + (row % 2) * brickWidth * 0.5;
      const by = y + row * brickHeight;
      ctx.fillStyle = colors[(row * 7 + column + 4) % colors.length];
      ctx.fillRect(bx + 1, by + 1, brickWidth - 2, brickHeight - 2);
      ctx.fillStyle = '#e3946c'; ctx.fillRect(bx + 2, by + 1, brickWidth - 4, 1);
    }
  }
  ctx.fillStyle = '#514d53'; ctx.fillRect(x, y, width, 5);
  ctx.fillStyle = '#c9b9a0'; ctx.fillRect(x + 2, y + 1, width - 4, 2);
  ctx.restore();
}
function face(ctx, variant, x, y, size, tilt = 0) {
  const sprite = assets.sprites[variant];
  ctx.save(); ctx.translate(x, y); ctx.rotate(tilt);
  ctx.imageSmoothingEnabled = false;
  ctx.drawImage(sprite, -size / 2, -size * sprite.height / sprite.width / 2, size, size * sprite.height / sprite.width);
  ctx.restore();
}
function aura(ctx, x, y, scale, time) {
  const pulse = Math.sin(time * 6) * 2;
  ctx.save(); ctx.translate(x, y); ctx.scale(scale, scale);
  const outline = [[-15,23],[-23,12],[-23,-8],[-18,-8],[-18,-22],[-11,-22],[-11,-30],[0,-39-pulse],[7,-28],[14,-28],[14,-18],[20,-18],[20,-7],[25,3],[21,17],[12,23]];
  for (let layer = 0; layer < 3; layer++) {
    ctx.save(); ctx.scale(1 - layer * 0.15, 1 - layer * 0.13);
    ctx.beginPath(); outline.forEach(([px, py], i) => i ? ctx.lineTo(px, py) : ctx.moveTo(px, py)); ctx.closePath();
    ctx.fillStyle = ['#ffb93022', '#ffd65c55', '#fff6ba44'][layer]; ctx.fill();
    ctx.strokeStyle = layer ? '#ffd15a55' : '#ffd35dbb'; ctx.lineWidth = 1; ctx.stroke(); ctx.restore();
  }
  ctx.fillStyle = '#ffe69c';
  for (let i = 0; i < 7; i++) ctx.fillRect((i % 2 ? -1 : 1) * (22 + i % 4), 23 - ((time * 30 + i * 9) % 63), 2, 3);
  ctx.restore();
}

function drawHero(time) {
  const canvas = document.querySelector('#hero-game'), ctx = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  ctx.imageSmoothingEnabled = false;
  ctx.fillStyle = '#263947'; ctx.fillRect(0, 0, w, h);
  ctx.drawImage(assets.city, 0, 0, w, h - 78);
  ctx.fillStyle = '#1f304522'; ctx.fillRect(0, 0, w, h);
  const x = 204 - (time * 25 % 320);
  for (const wx of [x, x + 320]) { wall(ctx, wx, 0, 49, 212); wall(ctx, wx, 357, 49, h - 78 - 357); }
  const by = 282 + Math.sin(time * 2) * 9;
  face(ctx, 0, 91, by, 43, Math.sin(time * 2) * 0.08);
  ctx.fillStyle = '#353f48'; ctx.fillRect(0, h - 78, w, 78);
  ctx.fillStyle = '#c4b6a0'; ctx.fillRect(0, h - 78, w, 4);
  ctx.fillStyle = '#e8bc62';
  for (let i = -1; i < 6; i++) ctx.fillRect(i * 64 - (time * 34 % 64), h - 41, 30, 3);
  ctx.textAlign = 'center';
  ctx.fillStyle = '#fff6d7'; ctx.font = '13px GamePixel, monospace'; ctx.fillText('NENCOINS', w / 2, 73);
  ctx.font = '39px GamePixel, monospace'; ctx.fillStyle = '#162332'; ctx.fillText('24', w / 2 + 1, 113);
  ctx.fillStyle = '#fff6d7'; ctx.fillText('24', w / 2, 111);
  ctx.font = '9px GamePixel, monospace'; ctx.fillStyle = '#c8d2d7'; ctx.fillText('CORRE NENGUE', w / 2, h - 15);
}

function drawPower(time) {
  const canvas = document.querySelector('#power-game'), ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.imageSmoothingEnabled = false;
  const cx = 200, cy = 221 + Math.sin(time * 2) * 7, wallX = 391;
  const gap = activePower === 'phone' ? 198 : 132;
  const upper = 215 - gap / 2, lower = 215 + gap / 2;
  const cycle = Math.max(0, time - powerStartedAt / 1000) % 3;
  const destroyed = activePower === 'bazooka' && cycle > 1;
  ctx.strokeStyle = '#454650'; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.moveTo(28, 361); ctx.lineTo(548, 361); ctx.stroke();
  if (!destroyed) { wall(ctx, wallX, -20, 59, upper + 20, true); wall(ctx, wallX, lower, 59, 360 - lower, true); }
  if (activePower === 'sayajin') aura(ctx, cx, cy, 2.2, time);
  face(ctx, 0, cx, cy, 79, Math.sin(time * 2) * 0.035);
  if (activePower === 'phone') {
    ctx.fillStyle = '#8be6ff'; ctx.font = '13px GamePixel, monospace'; ctx.textAlign = 'center';
    ctx.fillText('MAIS ESPAÇO', wallX + 29, 220);
    ctx.strokeStyle = '#8be6ff77'; ctx.setLineDash([3, 5]);
    ctx.beginPath(); ctx.moveTo(wallX + 78, upper + 10); ctx.lineTo(wallX + 78, lower - 10); ctx.stroke(); ctx.setLineDash([]);
    ctx.fillStyle = '#8be6ff'; ctx.fillRect(wallX + 74, upper + 8, 9, 3); ctx.fillRect(wallX + 74, lower - 11, 9, 3);
  }
  if (activePower === 'bazooka') {
    ctx.fillStyle = '#222c26'; ctx.fillRect(cx + 21, cy + 8, 62, 18); ctx.fillRect(cx + 37, cy + 22, 8, 14);
    ctx.fillStyle = '#8b9953'; ctx.fillRect(cx + 24, cy + 11, 54, 11);
    ctx.fillStyle = '#d4d6b2'; ctx.fillRect(cx + 24, cy + 11, 47, 3);
    if (cycle < 1) {
      const rx = cx + 86 + cycle * (wallX - cx - 80), ry = cy + 16 - cycle * 71;
      ctx.fillStyle = '#b6c67c'; ctx.fillRect(rx, ry, 16, 6);
      ctx.fillStyle = '#ffc65c'; ctx.fillRect(rx - 9, ry + 2, 7, 2);
    } else {
      const age = cycle - 1;
      if (age < 0.22) {
        ctx.fillStyle = '#ffd26f'; ctx.fillRect(wallX - 17, upper - 5, 92, 11); ctx.fillRect(wallX + 24, upper - 44, 10, 89);
      }
      if (age < 1.1) {
        ctx.globalAlpha = Math.min(1, (1.1 - age) * 5);
        for (let i = 0; i < 32; i++) {
          const px = wallX + (i % 6) * 10 + Math.sin(i * 3.9) * age * 122;
          const base = i < 16 ? upper : lower;
          const py = Math.min(353, base + Math.cos(i * 2.8) * 40 * age + age * age * 300);
          ctx.fillStyle = ['#b5724e', '#daa17c', '#805541'][i % 3]; ctx.fillRect(Math.round(px), Math.round(py), 8, 5);
        }
        ctx.globalAlpha = 1;
      }
    }
  }
  ctx.fillStyle = '#7b7d88'; ctx.textAlign = 'left'; ctx.font = '10px GamePixel, monospace';
  ctx.fillText('CORRE NENGUE / DUBBOX', 29, 45);
}
function drawCharacter() {
  if (!assets) return;
  const ctx = document.querySelector('#character-art').getContext('2d');
  ctx.clearRect(0, 0, 560, 500);
  face(ctx, activeColor, 280, 245, 280, -0.055);
}
let animationId = null;
let lastPaint = -Infinity;
const animationCanvases = [document.querySelector('#hero-game'), document.querySelector('#power-game')];
const visible = new Set(animationCanvases);
function drawFrame(now) {
  animationId = null;
  if (!assets || document.hidden) return;
  if (now - lastPaint >= 33 || reducedMotion.matches) {
    const time = reducedMotion.matches ? 1.4 : now / 1000;
    if (visible.has(animationCanvases[0])) drawHero(time);
    if (visible.has(animationCanvases[1])) drawPower(reducedMotion.matches ? powerStartedAt / 1000 + 0.8 : time);
    lastPaint = now;
  }
  if (!reducedMotion.matches && visible.size) animationId = requestAnimationFrame(drawFrame);
}
function requestDraw() {
  if (assets && animationId === null && !document.hidden) animationId = requestAnimationFrame(drawFrame);
}
if ('IntersectionObserver' in window) {
  const canvasObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => entry.isIntersecting ? visible.add(entry.target) : visible.delete(entry.target));
    requestDraw();
  }, { rootMargin: '120px' });
  animationCanvases.forEach(canvas => canvasObserver.observe(canvas));
}
reducedMotion.addEventListener('change', () => {
  if (reducedMotion.matches) {
    document.documentElement.classList.remove('motion-ready');
    if (animationId !== null) cancelAnimationFrame(animationId);
    animationId = null;
  }
  requestDraw();
});
Promise.all([imageAt('assets/character.png'), imageAt('assets/city.png')]).then(([character, city]) => {
  assets = { sprites: spritesFrom(character), city };
  drawCharacter();
  const icon = document.querySelector('#app-icon').getContext('2d');
  face(icon, 0, 60, 65, 88);
  redrawStatic = () => { lastPaint = -Infinity; requestDraw(); };
  requestDraw();
  if (document.fonts) document.fonts.ready.then(redrawStatic);
}).catch(() => {
  animationCanvases.forEach(canvas => {
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#fff4d9'; ctx.textAlign = 'center'; ctx.font = '16px sans-serif';
    ctx.fillText('Corre Nengue', canvas.width / 2, canvas.height / 2);
  });
});

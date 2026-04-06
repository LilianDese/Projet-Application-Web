const canvas = document.getElementById('rondFond');
const ctx = canvas.getContext('2d', { alpha: false });

const BG = '#050a24';

function initBackgroundMusic() {
  /** @type {HTMLAudioElement | null} */
  const audio = document.getElementById('bg-music');
  if (!audio) return;

  audio.volume = 0.009;

  const tryPlay = () => {
    if (!audio.paused) return;
    const p = audio.play();
    if (p && typeof p.then === 'function') {
      p.then(() => {
        window.removeEventListener('pointerdown', tryPlay, true);
        window.removeEventListener('keydown', tryPlay, true);
      }).catch(() => {
        // Autoplay still blocked until a user gesture; keep listeners
      });
    }
  };

  // Navigateurs: lecture audio autorisée uniquement après geste utilisateur
  window.addEventListener('pointerdown', tryPlay, true);
  window.addEventListener('keydown', tryPlay, true);
}

// Petit mouvement des cartes (JS léger) : update d'une variable CSS
const animCarte = Array.from(document.querySelectorAll('.card')).map((el) => {
  const css = getComputedStyle(el);
  const amp = parseFloat(css.getPropertyValue('--animCarte')) || 10;
  const period = 6 + Math.random() * 3; // 6..9s
  return {
    el,
    amp,
    omega: (2 * Math.PI) / period,
    phase: Math.random() * Math.PI * 2,
  };
});

function updateAnimCarte(timeSec) {
  for (const c of animCarte) {
    const y = Math.sin(timeSec * c.omega + c.phase) * c.amp;
    c.el.style.setProperty('--animCarteOffset', `${y.toFixed(2)}px`);
  }
}

// Palette de couleurs
const COLORS = [
  'rgba(120, 170, 255, 0.22)',
  'rgba(90, 220, 255, 0.16)',
  'rgba(170, 120, 255, 0.14)',
  'rgba(70, 140, 255, 0.18)',
];

/** @typedef {{x:number,y:number,r:number,baseVx:number,baseVy:number,fx:number,fy:number,phase:number,color:string}} Fond */

/** @type {Fond[]} */
let Fonds = [];
let width = 0;
let height = 0;
let dpr = 1;

function rand(min, max) {
  return Math.random() * (max - min) + min;
}

function clamp(v, min, max) {
  return Math.min(max, Math.max(min, v));
}

function resize() {
  width = Math.max(1, window.innerWidth);
  height = Math.max(1, window.innerHeight);
  dpr = Math.min(2, window.devicePixelRatio || 1);

  canvas.width = Math.floor(width * dpr);
  canvas.height = Math.floor(height * dpr);
  canvas.style.width = `${width}px`;
  canvas.style.height = `${height}px`;

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  const area = width * height;
  const count = Math.max(10, Math.min(18, Math.round(area / 120_000)));
  const minR = Math.max(250, Math.min(width, height) * 0.14);
  const maxR = Math.max(600, Math.min(width, height) * 0.34);

  Fonds = new Array(count).fill(null).map((_, i) => {
    const r = rand(minR, maxR);
    const slow = rand(0.02, 0.14); // vitesse

    return {
      x: rand(-r, width + r),
      y: rand(-r, height + r),
      r,
      baseVx: rand(-0.35, 0.35) * slow,
      baseVy: rand(-0.35, 0.35) * slow,
      fx: rand(0.12, 0.35),
      fy: rand(0.10, 0.32),
      phase: rand(0, Math.PI * 2) + i * 0.7,
      color: COLORS[i % COLORS.length],
    };
  });
}

function wrap(Fond) {
  const m = Fond.r * 1.8;
  if (Fond.x < -m) Fond.x = width + m;
  if (Fond.x > width + m) Fond.x = -m;
  if (Fond.y < -m) Fond.y = height + m;
  if (Fond.y > height + m) Fond.y = -m;
}

function drawFond(Fond) {
  const grad = ctx.createRadialGradient(Fond.x, Fond.y, 0, Fond.x, Fond.y, Fond.r);
  grad.addColorStop(0, Fond.color);
  grad.addColorStop(1, 'rgba(0, 0, 0, 0)');

  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(Fond.x, Fond.y, Fond.r, 0, Math.PI * 2);
  ctx.fill();
}

let last = performance.now();
let t = 0;

function frame(now) {
  const dt = Math.min(0.033, (now - last) / 1000);
  last = now;
  t += dt;

  // Fond
  ctx.globalCompositeOperation = 'source-over';
  ctx.fillStyle = BG;
  ctx.fillRect(0, 0, width, height);
  ctx.globalCompositeOperation = 'screen';

  for (const b of Fonds) {
    const ox = Math.sin(t * b.fx + b.phase) * 0.55;
    const oy = Math.cos(t * b.fy + b.phase) * 0.55;

    b.x += (b.baseVx + ox) * (60 * dt);
    b.y += (b.baseVy + oy) * (60 * dt);

    wrap(b);
    drawFond(b);
  }

  if (animCarte.length) updateAnimCarte(t);

  requestAnimationFrame(frame);
}

window.addEventListener('resize', resize, { passive: true });
resize();
initBackgroundMusic();
requestAnimationFrame(frame);

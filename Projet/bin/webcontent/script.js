const canvas = document.getElementById('rondFond');
const ctx = canvas.getContext('2d', { alpha: false });

const uiCanvas = document.getElementById('pixel-ui');
const uiCtx = uiCanvas.getContext('2d', { alpha: true });

// Taille des “pixels” du filtre pixelate (plus grand = plus pixelisé)
const PIXEL_SIZE = 5;
const uiLowCanvas = document.createElement('canvas');
const uiLowCtx = uiLowCanvas.getContext('2d');

const BG = '#050a24';

// Palette de couleurs
const COLORS = [
  'rgba(120, 170, 255, 0.22)',
  'rgba(90, 220, 255, 0.16)',
  'rgba(170, 120, 255, 0.14)',
  'rgba(70, 140, 255, 0.18)',
];

const panel = {
  x: 120,
  y: 90,
  w: 280,
  h: 160,
  r: 28,
};

let isDragging = false;
let dragPointerId = null;
let dragOffsetX = 0;
let dragOffsetY = 0;

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

function getPointerPosCss(e) {
  const rect = uiCanvas.getBoundingClientRect();
  return {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top,
  };
}

function hitPanel(x, y) {
  return x >= panel.x && x <= panel.x + panel.w && y >= panel.y && y <= panel.y + panel.h;
}

function roundedRectPath(c, x, y, w, h, r) {
  const rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
  c.beginPath();
  c.moveTo(x + rr, y);
  c.arcTo(x + w, y, x + w, y + h, rr);
  c.arcTo(x + w, y + h, x, y + h, rr);
  c.arcTo(x, y + h, x, y, rr);
  c.arcTo(x, y, x + w, y, rr);
  c.closePath();
}

uiCanvas.addEventListener(
  'pointerdown',
  (e) => {
    const p = getPointerPosCss(e);
    if (!hitPanel(p.x, p.y)) return;

    isDragging = true;
    dragPointerId = e.pointerId;
    dragOffsetX = p.x - panel.x;
    dragOffsetY = p.y - panel.y;
    uiCanvas.setPointerCapture(e.pointerId);
  },
  { passive: true },
);

uiCanvas.addEventListener(
  'pointermove',
  (e) => {
    if (!isDragging || e.pointerId !== dragPointerId) return;
    const p = getPointerPosCss(e);
    panel.x = clamp(p.x - dragOffsetX, 0, Math.max(0, width - panel.w));
    panel.y = clamp(p.y - dragOffsetY, 0, Math.max(0, height - panel.h));
  },
  { passive: true },
);

function stopDrag(e) {
  if (!isDragging) return;
  if (e && e.pointerId !== dragPointerId) return;
  isDragging = false;
  dragPointerId = null;
}

uiCanvas.addEventListener('pointerup', stopDrag, { passive: true });
uiCanvas.addEventListener('pointercancel', stopDrag, { passive: true });

function resize() {
  width = Math.max(1, window.innerWidth);
  height = Math.max(1, window.innerHeight);
  dpr = Math.min(2, window.devicePixelRatio || 1);

  canvas.width = Math.floor(width * dpr);
  canvas.height = Math.floor(height * dpr);
  canvas.style.width = `${width}px`;
  canvas.style.height = `${height}px`;

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  // Canvas UI pixelisé
  uiCanvas.width = Math.floor(width * dpr);
  uiCanvas.height = Math.floor(height * dpr);
  uiCanvas.style.width = `${width}px`;
  uiCanvas.style.height = `${height}px`;

  uiCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
  uiCtx.imageSmoothingEnabled = false;

  uiLowCanvas.width = Math.max(1, Math.floor(width / PIXEL_SIZE));
  uiLowCanvas.height = Math.max(1, Math.floor(height / PIXEL_SIZE));
  uiLowCtx.imageSmoothingEnabled = false;

  panel.x = clamp(panel.x, 0, Math.max(0, width - panel.w));
  panel.y = clamp(panel.y, 0, Math.max(0, height - panel.h));

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

  // UI pixelisée
  drawPixelUi();

  requestAnimationFrame(frame);
}

function drawPixelUi() {
  // Dessin UI en basse résolution
  uiLowCtx.setTransform(1, 0, 0, 1, 0, 0);
  uiLowCtx.clearRect(0, 0, uiLowCanvas.width, uiLowCanvas.height);

  // Dessiner en coordonnées CSS puis réduire via le transform
  uiLowCtx.setTransform(1 / PIXEL_SIZE, 0, 0, 1 / PIXEL_SIZE, 0, 0);
  roundedRectPath(uiLowCtx, panel.x, panel.y, panel.w, panel.h, panel.r);
  uiLowCtx.fillStyle = 'rgba(255, 255, 255, 0.18)';
  uiLowCtx.fill();
  uiLowCtx.lineWidth = 2;
  uiLowCtx.strokeStyle = 'rgba(190, 220, 255, 0.22)';
  uiLowCtx.stroke();

  //Upscale
  uiCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
  uiCtx.clearRect(0, 0, width, height);
  uiCtx.imageSmoothingEnabled = false;
  uiCtx.drawImage(uiLowCanvas, 0, 0, uiLowCanvas.width, uiLowCanvas.height, 0, 0, width, height);
}

window.addEventListener('resize', resize, { passive: true });
resize();
requestAnimationFrame(frame);

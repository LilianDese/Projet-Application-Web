const params       = new URLSearchParams(window.location.search);
const activeGameId = params.get('gameId');
const joueurId     = params.get('joueurId');
const currentPseudo = params.get('pseudo');

if (!activeGameId || !joueurId) {
  window.location.replace('./index.html');
}

const canvas = document.getElementById('rondFond');
const ctx    = canvas.getContext('2d', { alpha: false });
const BG     = '#050a24';

const COLORS = [
  'rgba(120, 170, 255, 0.22)',
  'rgba(90,  220, 255, 0.16)',
  'rgba(170, 120, 255, 0.14)',
  'rgba(70,  140, 255, 0.18)',
];

let Fonds = [], width = 0, height = 0, dpr = 1;

function rand(min, max) { return Math.random() * (max - min) + min; }

function resize() {
  width  = Math.max(1, window.innerWidth);
  height = Math.max(1, window.innerHeight);
  dpr    = Math.min(2, window.devicePixelRatio || 1);

  canvas.width  = Math.floor(width  * dpr);
  canvas.height = Math.floor(height * dpr);
  canvas.style.width  = `${width}px`;
  canvas.style.height = `${height}px`;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  const area  = width * height;
  const count = Math.max(10, Math.min(18, Math.round(area / 120_000)));
  const minR  = Math.max(250, Math.min(width, height) * 0.14);
  const maxR  = Math.max(600, Math.min(width, height) * 0.34);

  Fonds = Array.from({ length: count }, (_, i) => {
    const r    = rand(minR, maxR);
    const slow = rand(0.02, 0.14);
    return {
      x: rand(-r, width + r), y: rand(-r, height + r), r,
      baseVx: rand(-0.35, 0.35) * slow,
      baseVy: rand(-0.35, 0.35) * slow,
      fx: rand(0.12, 0.35), fy: rand(0.10, 0.32),
      phase: rand(0, Math.PI * 2) + i * 0.7,
      color: COLORS[i % COLORS.length],
    };
  });
}

function wrap(f) {
  const m = f.r * 1.8;
  if (f.x < -m)        f.x = width  + m;
  if (f.x > width  + m) f.x = -m;
  if (f.y < -m)        f.y = height + m;
  if (f.y > height + m) f.y = -m;
}

let last = performance.now(), t = 0;

function frame(now) {
  const dt = Math.min(0.033, (now - last) / 1000);
  last = now;
  t   += dt;

  ctx.globalCompositeOperation = 'source-over';
  ctx.fillStyle = BG;
  ctx.fillRect(0, 0, width, height);
  ctx.globalCompositeOperation = 'screen';

  for (const f of Fonds) {
    const ox = Math.sin(t * f.fx + f.phase) * 0.55;
    const oy = Math.cos(t * f.fy + f.phase) * 0.55;
    f.x += (f.baseVx + ox) * (60 * dt);
    f.y += (f.baseVy + oy) * (60 * dt);
    wrap(f);

    const g = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, f.r);
    g.addColorStop(0, f.color);
    g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(f.x, f.y, f.r, 0, Math.PI * 2);
    ctx.fill();
  }
  requestAnimationFrame(frame);
}

window.addEventListener('resize', resize, { passive: true });
resize();
requestAnimationFrame(frame);

function initAudio() {
  const audio = document.getElementById('bg-music');
  const slider = document.getElementById('volume');
  if (!audio) return;

  audio.volume = 0.05;
  audio.loop   = true;

  const tryPlay = async () => {
    if (!audio.paused) return;
    try { await audio.play(); } catch { /* autoplay bloqué */ }
  };

  const unmuteOnGesture = () => {
    audio.muted = false;
    window.removeEventListener('pointerdown', unmuteOnGesture, true);
    window.removeEventListener('keydown',     unmuteOnGesture, true);
  };

  (async () => {
    try { await audio.play(); }
    catch {
      audio.muted = true;
      try { await audio.play(); } catch { return; }
      window.addEventListener('pointerdown', unmuteOnGesture, true);
      window.addEventListener('keydown',     unmuteOnGesture, true);
    }
  })();

  window.addEventListener('pointerdown', () => void tryPlay(), true);
  window.addEventListener('keydown',     () => void tryPlay(), true);

  if (slider) {
    audio.volume = (Number(slider.value) || 0) / 100;
    slider.addEventListener('input', () => {
      audio.volume = Math.max(0, Math.min(1, Number(slider.value) / 100));
    });
  }
}

initAudio();

let gameTimer        = null;
let pendingWildCardId = null;
const CARD_IMG_BASE = 'assets/images';


function startPolling() {
  updateGameState();
  gameTimer = setInterval(updateGameState, 2000);
}

async function updateGameState() {
  if (!activeGameId || !joueurId) return;
  try {
    const res = await fetch(`./api/games/${activeGameId}/state?joueurId=${joueurId}`);
    if (!res.ok) return;
    renderGameState(await res.json());
  } catch (e) {
    console.error('Erreur fetch state:', e);
  }
}

function cardColorClass(color) {
  return { RED: 'card-red', BLUE: 'card-blue', GREEN: 'card-green', YELLOW: 'card-yellow', WILD: 'card-wild' }[color] ?? 'card-back';
}

function cardColorLabel(color) {
  return { RED: '🔴 Rouge', BLUE: '🔵 Bleu', GREEN: '🟢 Vert', YELLOW: '🟡 Jaune', WILD: '🃏 Joker' }[color] ?? color;
}

function cardLabel(card) {
  return { NUMBER: String(card.value), SKIP: '⊘', REVERSE: '↺', DRAW_TWO: '+2', WILD: '🃏', WILD_DRAW_FOUR: '+4' }[card.type] ?? '?';
}

function normalizeColorName(color) {
  return { RED: 'Red', BLUE: 'Blue', GREEN: 'Green', YELLOW: 'Yellow' }[color] ?? 'Red';
}

function cardImageFile(card) {
  if (!card || !card.type) return 'Deck.png';

  if (card.type === 'WILD') return 'Wild.png';
  if (card.type === 'WILD_DRAW_FOUR') return 'Wild_Draw.png';

  const color = normalizeColorName(card.color);
  if (card.type === 'NUMBER') return `${color}_${card.value}.png`;
  if (card.type === 'SKIP') return `${color}_Skip.png`;
  if (card.type === 'REVERSE') return `${color}_Reverse.png`;
  if (card.type === 'DRAW_TWO') return `${color}_Draw.png`;
  return 'Deck.png';
}

function cardImageSrc(card) {
  return `${CARD_IMG_BASE}/${cardImageFile(card)}`;
}

function makeCardImage(card, altText) {
  const img = document.createElement('img');
  img.className = 'card-img';
  img.src = cardImageSrc(card);
  img.alt = altText;
  img.draggable = false;
  return img;
}

function isCardPlayable(card, currentColor, topCard) {
  if (card.type === 'WILD' || card.type === 'WILD_DRAW_FOUR') return true;
  if (card.color === currentColor) return true;
  if (topCard) {
    if (card.type !== 'NUMBER' && card.type === topCard.type) return true;
    if (card.type === 'NUMBER' && topCard.type === 'NUMBER' && card.value === topCard.value) return true;
  }
  return false;
}

function renderGameState(state) {
  const titleEl = document.getElementById('game-title');
  if (titleEl) titleEl.textContent = `Partie #${state.gameId}`;

  const dirEl = document.getElementById('game-direction');
  if (dirEl) dirEl.textContent = state.direction === 1 ? '→ sens horaire' : '← sens anti-horaire';

  const topCardEl = document.getElementById('game-top-card');
  if (topCardEl) {
    topCardEl.className = 'card-visual card-large card-back';
    topCardEl.innerHTML = '';
    if (state.topCard) {
      topCardEl.appendChild(makeCardImage(state.topCard, 'Carte au sommet de la défausse'));
    } else {
      topCardEl.appendChild(makeCardImage(null, 'Pioche'));
    }
  }

  //Couleur active
  const colorEl = document.getElementById('game-current-color');
  if (colorEl) colorEl.textContent = `Couleur : ${cardColorLabel(state.currentColor)}`;

  //Indicateur de tour
  const turnEl = document.getElementById('game-current-turn');
  if (turnEl) {
    if (state.myTurn) {
      turnEl.textContent   = '▶ C\'est votre tour !';
      turnEl.style.color   = '#4dff91';
    } else {
      turnEl.textContent   = `Tour de : ${state.currentPlayerPseudo}`;
      turnEl.style.color   = 'rgba(255,255,255,0.65)';
    }
  }

  //Joueurs
  const playersEl = document.getElementById('game-players');
  if (playersEl) {
    playersEl.innerHTML = '';
    for (const p of (state.players ?? [])) {
      const span = document.createElement('span');
      span.className   = 'game-player' + (p.currentPlayer ? ' game-player-active' : '');
      span.textContent = `${p.pseudo} (${p.handSize})${p.hasCalledUno ? ' · UNO!' : ''}`;
      playersEl.appendChild(span);
    }
  }

  //Main du joueur
  const handEl = document.getElementById('game-hand');
  if (handEl) {
    handEl.innerHTML = '';
    for (const card of (state.myHand ?? [])) {
      const playable = state.myTurn && isCardPlayable(card, state.currentColor, state.topCard);
      const btn      = document.createElement('button');
      btn.type        = 'button';
      btn.className   = `card-visual ${playable ? 'card-playable' : 'card-unplayable'}`;
      btn.title       = `${card.color} ${card.type}${card.type === 'NUMBER' ? ' ' + card.value : ''}`;
      btn.appendChild(makeCardImage(card, btn.title));
      if (playable) btn.onclick = () => onCardClick(card);
      handEl.appendChild(btn);
    }
  }

  //Boutons action
  const btnDraw = document.getElementById('btn-game-draw');
  if (btnDraw) btnDraw.disabled = !state.myTurn;
  const btnUno = document.getElementById('btn-game-uno');
  if (btnUno)  btnUno.disabled  = !state.myTurn;

  // Victoire
  if (state.status === 'FINISHED') {
    if (gameTimer) { clearInterval(gameTimer); gameTimer = null; }
    const textEl    = document.getElementById('game-victory-text');
    const victoryEl = document.getElementById('game-victory');
    if (textEl)    textEl.textContent    = state.winnerPseudo === currentPseudo ? '🎉 Vous avez gagné !' : `🏆 Victoire de ${state.winnerPseudo}`;
    if (victoryEl) victoryEl.style.display = 'flex';
  }
}

function onCardClick(card) {
  if (card.type === 'WILD' || card.type === 'WILD_DRAW_FOUR') {
    pendingWildCardId = card.id;
    document.getElementById('color-picker').style.display = 'flex';
  } else {
    void playCard(card.id, null);
  }
}

async function playCard(cardId, chosenColor) {
  let url = `./api/games/${activeGameId}/play?joueurId=${joueurId}&cardId=${cardId}`;
  if (chosenColor) url += `&chosenColor=${chosenColor}`;
  try {
    const res = await fetch(url, { method: 'POST' });
    if (!res.ok) { console.error('play error', res.status, await res.text()); return; }
    renderGameState(await res.json());
  } catch (e) { console.error('Erreur play:', e); }
}

async function drawCard() {
  try {
    const res = await fetch(`./api/games/${activeGameId}/draw?joueurId=${joueurId}`, { method: 'POST' });
    if (!res.ok) { console.error('draw error', res.status, await res.text()); return; }
    renderGameState(await res.json());
  } catch (e) { console.error('Erreur draw:', e); }
}

async function callUno() {
  try {
    await fetch(`./api/games/${activeGameId}/uno?joueurId=${joueurId}`, { method: 'POST' });
  } catch (e) { console.error('Erreur uno:', e); }
}

function onColorChosen(color) {
  document.getElementById('color-picker').style.display = 'none';
  if (pendingWildCardId) {
    void playCard(pendingWildCardId, color);
    pendingWildCardId = null;
  }
}


document.getElementById('btn-game-quit')?.addEventListener('click', () => {
  if (gameTimer) clearInterval(gameTimer);
  window.location.href = './index.html';
});
document.getElementById('btn-game-draw')?.addEventListener('click', () => void drawCard());
document.getElementById('btn-game-uno')?.addEventListener('click',  () => void callUno());
document.getElementById('btn-victory-quit')?.addEventListener('click', () => {
  window.location.href = './index.html';
});


startPolling();

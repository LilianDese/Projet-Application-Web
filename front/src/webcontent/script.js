const canvas = document.getElementById('rondFond');
const ctx = canvas.getContext('2d', { alpha: false });

const BG = '#050a24';

let authMode = null; // 'login' | 'register'
let currentPseudo = null;
let messageTimer = null;

function initBackgroundMusic() {
  /** @type {HTMLAudioElement | null} */
  const audio = document.getElementById('bg-music');
  if (!audio) return;

  audio.volume = 0.050;
  audio.loop = true;
  audio.preload = 'auto';

  // NOTE: Les navigateurs modernes peuvent bloquer l'audio sans geste utilisateur.
  // Ici: best-effort -> on tente autoplay, sinon on démarre en muet, puis on démute au 1er geste.
  const tryPlay = async () => {
    if (!audio.paused) return true;
    try {
      await audio.play();
      return true;
    } catch {
      return false;
    }
  };

  const start = async () => {
    const ok = await tryPlay();
    if (ok) return;

    // fallback: autoplay muet
    const wasMuted = audio.muted;
    audio.muted = true;
    const okMuted = await tryPlay();
    if (!okMuted) {
      audio.muted = wasMuted;
      return;
    }

    // démute dès qu'on a un geste utilisateur
    const unmuteOnGesture = () => {
      audio.muted = false;
      window.removeEventListener('pointerdown', unmuteOnGesture, true);
      window.removeEventListener('keydown', unmuteOnGesture, true);
    };
    window.addEventListener('pointerdown', unmuteOnGesture, true);
    window.addEventListener('keydown', unmuteOnGesture, true);
  };

  // Navigateurs: lecture audio autorisée uniquement après geste utilisateur
  window.addEventListener('pointerdown', () => { void tryPlay(); }, true);
  window.addEventListener('keydown', () => { void tryPlay(); }, true);

  // Tente un démarrage immédiat (best-effort)
  void start();
}

function initVolumeSlider() {
  const audio = document.getElementById('bg-music');
  const slider = document.getElementById('volume');
  if (!audio || !slider) return;

  const setVolume = (v01) => {
    audio.volume = Math.max(0, Math.min(1, v01));
  };

  setVolume((Number(slider.value) || 0) / 100);
  slider.addEventListener('input', () => {
    setVolume((Number(slider.value) || 0) / 100);
  });
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
initVolumeSlider();
requestAnimationFrame(frame);

function setMessage(text, isError) {
  const el = document.getElementById('auth-message');
  if (!el) return;

  if (messageTimer) {
    clearTimeout(messageTimer);
    messageTimer = null;
  }

  el.textContent = text || '';
  el.classList.toggle('error', !!isError);

  if (text) {
    const ms = isError ? 5000 : 3200;
    messageTimer = setTimeout(() => {
      el.textContent = '';
      el.classList.remove('error');
      messageTimer = null;
    }, ms);
  }
}

function setPartMessage(text, isError) {
  const el = document.getElementById('part-message');
  if (!el) return;

  if (messageTimer) {
    clearTimeout(messageTimer);
    messageTimer = null;
  }

  el.textContent = text || '';
  el.classList.toggle('error', !!isError);

  if (text) {
    const ms = isError ? 5000 : 3200;
    messageTimer = setTimeout(() => {
      el.textContent = '';
      el.classList.remove('error');
      messageTimer = null;
    }, ms);
  }
}

function setCurrentUser(pseudo) {
  const el = document.getElementById('current-user');
  if (!el) return;
  el.textContent = pseudo ? `Connecté : ${pseudo}` : '';
}

function setLoggedIn(pseudo) {
  currentPseudo = pseudo;
  setCurrentUser(pseudo);

  const actions = document.getElementById('menu-actions');
  const form = document.getElementById('auth-form');
  const logout = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const listGamesBtn = document.getElementById('btn-list-games');
  const createGameBtn = document.getElementById('btn-create-game');
  if (actions) actions.style.display = 'none';
  if (form) form.style.display = 'none';
  if (logout) logout.style.display = 'inline-block';
  if (leaderboardBtn) leaderboardBtn.style.display = 'inline-block';
  if (listGamesBtn) listGamesBtn.style.display = 'inline-block';
  if (createGameBtn) createGameBtn.style.display = 'inline-block';
}

function setLoggedOut() {
  currentPseudo = null;
  setCurrentUser('');
  setMessage('', false);

  const actions = document.getElementById('menu-actions');
  const form = document.getElementById('auth-form');
  const input = document.getElementById('pseudo');
  const logout = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const listGamesBtn = document.getElementById('btn-list-games');
  const createGameBtn = document.getElementById('btn-create-game');
  const overlay = document.getElementById('leaderboard-overlay');
  const gamesOverlay = document.getElementById('games-overlay');
  if (actions) actions.style.display = 'flex';
  if (form) form.style.display = 'none';
  if (input) input.value = '';
  if (logout) logout.style.display = 'none';
  if (leaderboardBtn) leaderboardBtn.style.display = 'none';
  if (listGamesBtn) listGamesBtn.style.display = 'none';
  if (createGameBtn) createGameBtn.style.display = 'none';
  if (overlay) overlay.style.display = 'none';
  if (gamesOverlay) gamesOverlay.style.display = 'none';
}

function renderLeaderboard(joueurs) {
  const container = document.getElementById('leaderboard-content');
  if (!container) return;

  if (!Array.isArray(joueurs) || joueurs.length === 0) {
    container.textContent = 'Aucun joueur';
    return;
  }

  const ul = document.createElement('ul');
  ul.className = 'leaderboard-list';
  for (const j of joueurs) {
    const li = document.createElement('li');
    li.textContent = j?.pseudo ?? String(j?.id ?? '');
    ul.appendChild(li);
  }
  container.innerHTML = '';
  container.appendChild(ul);
}

async function openLeaderboard() {
  if (!currentPseudo) return;
  const overlay = document.getElementById('leaderboard-overlay');
  const container = document.getElementById('leaderboard-content');
  if (!overlay || !container) return;

  overlay.style.display = 'grid';
  container.textContent = 'Chargement...';
  try {
    const joueurs = await fetchJoueurs();
    renderLeaderboard(joueurs);
  } catch {
    container.textContent = 'Erreur réseau';
  }
}

function closeLeaderboard() {
  const overlay = document.getElementById('leaderboard-overlay');
  if (overlay) overlay.style.display = 'none';
}

function showAuthForm(mode) {
  authMode = mode;
  const form = document.getElementById('auth-form');
  const submit = document.getElementById('btn-submit');
  const input = document.getElementById('pseudo');
  if (!form || !submit || !input) return;

  if (currentPseudo) return;

  form.style.display = 'flex';
  submit.textContent = mode === 'login' ? 'Se connecter' : 'Créer';
  setMessage('', false);
  input.focus();
}

function showPartForm() {
  const form = document.getElementById('part-form');
  const input = document.getElementById('part-name');
  if (!form || !input) return;

  form.style.display = 'flex';
  setPartMessage('', false);
  input.focus();
}

async function fetchJoueurs() {
  const res = await fetch('/back/joueurs', { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(String(res.status));
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

async function fetchGames() {
  const res = await fetch('/back/games', { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(String(res.status));
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

function renderGames(games) {
  const container = document.getElementById('games-content');
  if (!container) return;

  if (!Array.isArray(games) || games.length === 0) {
    container.textContent = 'Aucune partie trouvée';
    return;
  }

  const ul = document.createElement('ul');
  ul.className = 'leaderboard-list';
  for (const game of games) {
    const li = document.createElement('li');
    const title = game?.name ? `${game.name} (ID ${game.id})` : `Partie ${game.id}`;
    li.textContent = `${title} · ${game.status} · ${game.playerCount} joueur(s)`;
    ul.appendChild(li);
  }

  container.innerHTML = '';
  container.appendChild(ul);
}

async function openGamesList() {
  if (!currentPseudo) return;
  const overlay = document.getElementById('games-overlay');
  const container = document.getElementById('games-content');
  if (!overlay || !container) return;

  overlay.style.display = 'grid';
  container.textContent = 'Chargement...';
  try {
    const games = await fetchGames();
    renderGames(games);
  } catch {
    container.textContent = 'Erreur réseau';
  }
}

function closeGamesList() {
  const overlay = document.getElementById('games-overlay');
  if (overlay) overlay.style.display = 'none';
}

async function createGame(name) {
  const res = await fetch('/back/games', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ name }),
  });
  return res;
}

async function askCreateGame() {
  if (!currentPseudo) return;
  showPartForm();
}

async function createJoueur(pseudo) {
  const res = await fetch('/back/joueurs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ pseudo, password: '' }),
  });
  return res;
}

function normalizePseudo(value) {
  const s = (value ?? '').trim();
  return s.length ? s : null;
}

function initAuthUi() {
  const loginBtn = document.getElementById('btn-login');
  const regBtn = document.getElementById('btn-register');
  const form = document.getElementById('auth-form');
  const input = document.getElementById('pseudo');
  const logoutBtn = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const listGamesBtn = document.getElementById('btn-list-games');
  const createGameBtn = document.getElementById('btn-create-game');
  const leaderboardBackBtn = document.getElementById('btn-leaderboard-back');
  const gamesBackBtn = document.getElementById('btn-games-back');
  if (!loginBtn || !regBtn || !form || !input) return;

  loginBtn.addEventListener('click', () => showAuthForm('login'));
  regBtn.addEventListener('click', () => showAuthForm('register'));

  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      setLoggedOut();
    });
  }

  if (leaderboardBtn) {
    leaderboardBtn.addEventListener('click', () => {
      void openLeaderboard();
    });
  }

  if (listGamesBtn) {
    listGamesBtn.addEventListener('click', () => {
      void openGamesList();
    });
  }

  if (createGameBtn) {
    createGameBtn.addEventListener('click', () => {
      void askCreateGame();
    });
  }

  if (leaderboardBackBtn) {
    leaderboardBackBtn.addEventListener('click', () => {
      closeLeaderboard();
    });
  }

  if (gamesBackBtn) {
    gamesBackBtn.addEventListener('click', () => {
      closeGamesList();
    });
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const pseudo = normalizePseudo(input.value);
    if (!pseudo) {
      setMessage('Pseudo requis', true);
      return;
    }

    try {
      if (authMode === 'login') {
        const joueurs = await fetchJoueurs();
        const exists = joueurs.some((j) => (j?.pseudo ?? '').toLowerCase() === pseudo.toLowerCase());
        if (!exists) {
          setMessage('Pseudo introuvable', true);
          return;
        }
        setLoggedIn(pseudo);
        setMessage('Connexion OK', false);
        return;
      }

      if (authMode === 'register') {
        const res = await createJoueur(pseudo);
        if (res.status === 201) {
          setLoggedIn(pseudo);
          setMessage('Joueur créé', false);
          return;
        }
        if (res.status === 409) {
          setMessage('Pseudo déjà utilisé', true);
          return;
        }
        const txt = await res.text();
        setMessage(`Erreur (${res.status}) ${txt || ''}`.trim(), true);
        return;
      }
    } catch (err) {
      setMessage('Erreur réseau', true);
    }
  });
}

const partForm = document.getElementById('part-form');
const partInput = document.getElementById('part-name');
if (partForm && partInput) {
  partForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = normalizePseudo(partInput.value);
    if (!name) {
      setPartMessage('Nom de partie requis', true);
      return;
    }

    try {
      const res = await createGame(name);
      if (res.status === 201) {
        const data = await res.json();
        setPartMessage(`Partie créée : ${data.name ?? data.id}`, false);
        partForm.style.display = 'none';
        partInput.value = '';
        return;
      }
      const txt = await res.text();
      setPartMessage(`Erreur création partie (${res.status}) ${txt || ''}`.trim(), true);
    } catch {
      setPartMessage('Erreur réseau', true);
    }
  });
}

initAuthUi();

const canvas = document.getElementById('rondFond');
const ctx = canvas.getContext('2d', { alpha: false });

const BG = '#050a24';

let authMode = null; // 'login' | 'register'
let currentPseudo = null;
let currentJoueurId = null;
let messageTimer = null;


//=======================================
//MUSIQUE
//=======================================

//init
function initBackgroundMusic() {
  /** @type {HTMLAudioElement | null} */
  const audio = document.getElementById('bg-music');
  if (!audio) return;

  audio.volume = 0.050;
  audio.loop = true;
  audio.preload = 'auto';

  //jouer dès le début si possible
  const tryPlay = async () => {
    if (!audio.paused) return true;
    try {
      await audio.play();
      return true;
    } catch {
      return false;
    }
  };

//débute la musique
  const start = async () => {
    const ok = await tryPlay();
    if (ok) return;

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

  window.addEventListener('pointerdown', () => { void tryPlay(); }, true);
  window.addEventListener('keydown', () => { void tryPlay(); }, true);

 
  void start();
}


//bouton de volume
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


//=====================================
//ANIMATIONS CARTES
//=====================================

// Petit mouvement des cartes
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


//=======================================
//FONDS ANIME
//=======================================

//couleurs
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


//initialisation des fonds
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
//fait rebondir les fonds sur les bords
function wrap(Fond) {
  const m = Fond.r * 1.8;
  if (Fond.x < -m) Fond.x = width + m;
  if (Fond.x > width + m) Fond.x = -m;
  if (Fond.y < -m) Fond.y = height + m;
  if (Fond.y > height + m) Fond.y = -m;
}

//dessine un fond
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

//boucle d'animation
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

//joue qd on ouvre la page ou qd on redimensionne
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
//===============================
//GESTION DE LOGIN ET AFFICHAGE
//===============================
function setCurrentUser(pseudo) {
  const el = document.getElementById('current-user');
  if (!el) return;
  el.textContent = pseudo ? `Connecté : ${pseudo}` : '';
}

//ecran après login
function setLoggedIn(pseudo, id) {
  currentPseudo = pseudo;
  currentJoueurId = id;
  setCurrentUser(pseudo);

  const actions = document.getElementById('menu-actions');
  const form = document.getElementById('auth-form');
  const logout = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const createGameBtn = document.getElementById('btn-create-game');
  const gamesDock = document.getElementById('games-dock');
  if (actions) actions.style.display = 'none';
  if (form) form.style.display = 'none';
  if (logout) logout.style.display = 'inline-block';
  if (leaderboardBtn) leaderboardBtn.style.display = 'inline-block';
  if (createGameBtn) createGameBtn.style.display = 'inline-block';
  if (gamesDock) gamesDock.style.display = 'block';

  void refreshGamesDock();
}

//ecran après logout
function setLoggedOut() {
  currentPseudo = null;
  currentJoueurId = null;
  setCurrentUser('');
  setMessage('', false);

  const actions = document.getElementById('menu-actions');
  const form = document.getElementById('auth-form');
  const input = document.getElementById('pseudo');
  const logout = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const createGameBtn = document.getElementById('btn-create-game');
  const overlay = document.getElementById('leaderboard-overlay');
  const gamesDock = document.getElementById('games-dock');
  const partForm = document.getElementById('part-form');
  const partInput = document.getElementById('part-name');
  if (actions) actions.style.display = 'flex';
  if (form) form.style.display = 'none';
  if (input) input.value = '';
  if (logout) logout.style.display = 'none';
  if (leaderboardBtn) leaderboardBtn.style.display = 'none';
  if (createGameBtn) createGameBtn.style.display = 'none';
  if (overlay) overlay.style.display = 'none';
  if (gamesDock) gamesDock.style.display = 'none';
  if (partForm) partForm.style.display = 'none';
  if (partInput) partInput.value = '';
  setPartMessage('', false);
}

//leadearboard
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

//ouvre le leaderboard
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
//ferme le leaderboard
function closeLeaderboard() {
  const overlay = document.getElementById('leaderboard-overlay');
  if (overlay) overlay.style.display = 'none';
}

//Form de connection ou nouveau joueur
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
//get les joueurs
async function fetchJoueurs() {
  const res = await fetch('./api/joueurs', { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(String(res.status));
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

//get les parties
async function fetchGames() {
  const res = await fetch('./api/games', { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(String(res.status));
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

function renderGames(games) {
  const container = document.getElementById('games-content');
  if (!container) return;

  container.innerHTML = '';

  if (!Array.isArray(games) || games.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'games-empty';
    empty.textContent = 'Aucune partie trouvée';
    container.appendChild(empty);
    return;
  }

  //========================
  //AFFICHAGE DES PARTIES
  //========================
  for (const game of games) {
    const card = document.createElement('div');
    card.className = 'game-card';

    const titleText = game?.name ? `${game.name}` : `Partie ${game?.id ?? ''}`.trim();
    const title = document.createElement('div');
    title.className = 'game-title';
    title.textContent = game?.id != null ? `${titleText} (ID ${game.id})` : titleText;
    card.appendChild(title);

    const meta = document.createElement('div');
    meta.className = 'game-meta';
    meta.textContent = `${game?.status ?? ''} · ${game?.playerCount ?? 0} joueur(s)`;
    card.appendChild(meta);

    if (game?.status === 'LOBBY') {
      const btn = document.createElement('button');
      btn.className = 'btn btn-small';
      btn.textContent = 'Rejoindre';
      btn.onclick = () => joinGameAndLobby(game.id);
      card.appendChild(btn);
    }

    container.appendChild(card);
  }
}
//redresh la liste des parties
async function refreshGamesDock() {
  if (!currentPseudo) return;
  const container = document.getElementById('games-content');
  if (!container) return;

  container.innerHTML = '';
  const loading = document.createElement('div');
  loading.className = 'games-empty';
  loading.textContent = 'Chargement...';
  container.appendChild(loading);

  try {
    const games = await fetchGames();
    renderGames(games);
  } catch {
    loading.textContent = 'Erreur réseau';
  }
}

async function createGame(name) {
  const res = await fetch('./api/games', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ name, creatorId: currentJoueurId }),
  });
  return res;
}

//ajoute une partie
async function askCreateGame() {
  if (!currentPseudo) return;
  showPartForm();
}

//ajoute un joueur
async function createJoueur(pseudo) {
  const res = await fetch('./api/joueurs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ pseudo, password: '' }),
  });
  return res;
}
//crop pseudosi trop long
function normalizePseudo(value) {
  const s = (value ?? '').trim();
  return s.length ? s : null;
}
//Ecran de  login ou de création de compte
function initAuthUi() {
  const loginBtn = document.getElementById('btn-login');
  const regBtn = document.getElementById('btn-register');
  const form = document.getElementById('auth-form');
  const input = document.getElementById('pseudo');
  const logoutBtn = document.getElementById('btn-logout');
  const leaderboardBtn = document.getElementById('btn-leaderboard');
  const createGameBtn = document.getElementById('btn-create-game');
  const leaderboardBackBtn = document.getElementById('btn-leaderboard-back');
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
        const joueur = joueurs.find((j) => (j?.pseudo ?? '').toLowerCase() === pseudo.toLowerCase());
        if (!joueur) {
          setMessage('Pseudo introuvable', true);
          return;
        }
        setLoggedIn(joueur.pseudo, joueur.id);
        setMessage('Connexion OK', false);
        return;
      }

      if (authMode === 'register') {
        const res = await createJoueur(pseudo);
        if (res.status === 201) {
          const data = await res.json();
          setLoggedIn(data.pseudo, data.id);
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
        void refreshGamesDock();
        openLobby(data.id);
        return;
      }
      const txt = await res.text();
      setPartMessage(`Erreur création partie (${res.status}) ${txt || ''}`.trim(), true);
    } catch {
      setPartMessage('Erreur réseau', true);
    }
  });
}

//========================
//Gestion de lobby
//========================

let lobbyTimer = null;
let activeLobbyGameId = null;

async function joinGame(gameId) {
  const res = await fetch(`./api/games/${gameId}/join?joueurId=${currentJoueurId}`, { method: 'POST' });
  if (!res.ok) throw new Error('Erreur join');
  return res;
}

async function joinGameAndLobby(gameId) {
   try {
     await joinGame(gameId);
     void refreshGamesDock();
     openLobby(gameId);
   } catch(e) {
     alert('Erreur: impossible de rejoindre. ' + e.message);
   }
}

async function fetchGame(gameId) {
  const res = await fetch(`./api/games/${gameId}`);
  if (!res.ok) throw new Error('Erreur fetch game');
  return await res.json();
}

async function startGame(gameId) {
  const res = await fetch(`./api/games/${gameId}/start?joueurId=${currentJoueurId}`, { method: 'POST' });
  if (!res.ok) {
     const t = await res.text();
     alert('Erreur: ' + (t || 'Impossible de lancer la partie'));
  }
}

function openLobby(gameId) {
  activeLobbyGameId = gameId;
  const overlay = document.getElementById('lobby-overlay');
  if (overlay) overlay.style.display = 'grid';
  
  if (lobbyTimer) clearInterval(lobbyTimer);
  updateLobby();
  lobbyTimer = setInterval(updateLobby, 2000);
}

function closeLobby() {
  activeLobbyGameId = null;
  if (lobbyTimer) clearInterval(lobbyTimer);
  const overlay = document.getElementById('lobby-overlay');
  if (overlay) overlay.style.display = 'none';
}

async function updateLobby() {
   if (!activeLobbyGameId) return;
   try {
     const game = await fetchGame(activeLobbyGameId);
     document.getElementById('lobby-title').textContent = `Salon: ${game.name || game.id}`;
     
     if (game.status === 'IN_PROGRESS') {
       closeLobby();
       alert('Partie lancée - Le jeu commence !');
       return;
     }
     
     const ul = document.getElementById('lobby-players');
     if (ul) {
       ul.innerHTML = '';
       for (const p of (game.playerPseudos || [])) {
         const li = document.createElement('li');
         li.textContent = p;
         ul.appendChild(li);
       }
     }
     
     const btnStart = document.getElementById('btn-lobby-start');
     if (btnStart) {
       if (game.creatorId === currentJoueurId) {
          btnStart.style.display = 'inline-block';
          btnStart.disabled = game.playerCount < 3;
       } else {
          btnStart.style.display = 'none';
       }
     }
   } catch (e) {
     console.error(e);
   }
}

document.getElementById('btn-lobby-quit')?.addEventListener('click', closeLobby);
document.getElementById('btn-lobby-start')?.addEventListener('click', () => {
    if (activeLobbyGameId) startGame(activeLobbyGameId);
});

initAuthUi();

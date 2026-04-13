package n7.facade.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import n7.facade.model.Card;
import n7.facade.model.CardColor;
import n7.facade.model.CardType;
import n7.facade.model.Game;
import n7.facade.model.GamePlayer;
import n7.facade.model.GameStatus;
import n7.facade.model.HandCard;
import n7.facade.model.Joueur;
import n7.facade.repository.CardRepository;
import n7.facade.repository.GamePlayerRepository;
import n7.facade.repository.GameRepository;
import n7.facade.repository.HandCardRepository;
import n7.facade.repository.JoueurRepository;
import n7.facade.web.dto.CardDto;
import n7.facade.web.dto.CreateGameRequest;
import n7.facade.web.dto.FacadeJoueurRequest;
import n7.facade.web.dto.FacadeJoueurResponse;
import n7.facade.web.dto.GameResponse;
import n7.facade.web.dto.GameStateResponse;
import n7.facade.web.dto.JoinGameResponse;
import n7.facade.web.dto.PlayerStateDto;

/**
 * Facade REST 
 */
@RestController
public class FacadeController {
    
	private final JoueurRepository joueurRepository;
	private final GameRepository gameRepository;
	private final GamePlayerRepository gamePlayerRepository;
	private final CardRepository cardRepository;
	private final HandCardRepository handCardRepository;

	public FacadeController(
			JoueurRepository joueurRepository,
			GameRepository gameRepository,
			GamePlayerRepository gamePlayerRepository,
			CardRepository cardRepository,
			HandCardRepository handCardRepository) {
		this.joueurRepository = joueurRepository;
		this.gameRepository = gameRepository;
		this.gamePlayerRepository = gamePlayerRepository;
		this.cardRepository = cardRepository;
		this.handCardRepository = handCardRepository;
	}

	// ---- joueurs ----

	@PostMapping("/joueurs")
	@Transactional
	public ResponseEntity<FacadeJoueurResponse> addJoueur(@RequestBody FacadeJoueurRequest request) {
		String pseudo = normalize(request.getPseudo());
		if (pseudo == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pseudo requis");
		}
		if (joueurRepository.existsByPseudo(pseudo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "pseudo déjà utilisé");
		}

		Joueur saved = joueurRepository.save(new Joueur(pseudo, request.getPassword()));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new FacadeJoueurResponse(saved.getId(), saved.getPseudo()));
	}

	@GetMapping("/joueurs")
	public List<FacadeJoueurResponse> listJoueurs() {
		return joueurRepository.findAll().stream()
				.map(j -> new FacadeJoueurResponse(j.getId(), j.getPseudo()))
				.collect(Collectors.toList());
	}

	// ---- games ----

	@PostMapping("/games")
	@Transactional
	public ResponseEntity<GameResponse> createGame(@RequestBody CreateGameRequest request) {
		String name = normalize(request.getName());
		if (name == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nom de partie requis");
		}
		Game game = new Game(GameStatus.LOBBY, name);

		if (request.getCreatorId() != null) {
			Joueur creator = joueurRepository.findById(request.getCreatorId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "créateur introuvable"));
			game.setCreator(creator);
			game = gameRepository.save(game);
			
			GamePlayer participation = new GamePlayer(game, creator, 0);
			game.addPlayer(participation);
			gamePlayerRepository.save(participation);
		} else {
			game = gameRepository.save(game);
		}

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(game));
	}

	@GetMapping("/games")
	public List<GameResponse> listGames(@RequestParam(name = "status", required = false) GameStatus status) {
		List<Game> games = (status == null) ? gameRepository.findAll() : gameRepository.findByStatus(status);
		return games.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	@GetMapping("/games/{gameId}")
	public ResponseEntity<GameResponse> getGame(@PathVariable("gameId") Long gameId) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));
		return ResponseEntity.ok(toResponse(game));
	}

	@PostMapping("/games/{gameId}/join")
	@Transactional
	public ResponseEntity<JoinGameResponse> joinGame(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		Joueur joueur = joueurRepository.findById(joueurId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "joueur introuvable"));

		int turnOrder = game.getPlayers().size();
		GamePlayer participation = new GamePlayer(game, joueur, turnOrder);
		game.addPlayer(participation);
		GamePlayer saved = gamePlayerRepository.save(participation);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new JoinGameResponse(saved.getId(), game.getId(), joueur.getId(), saved.getTurnOrder()));
	}

	@PostMapping("/games/{gameId}/start")
	@Transactional
	public ResponseEntity<GameResponse> startGame(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId) {

		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		if (game.getStatus() != GameStatus.LOBBY) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La partie n'est pas dans le salon");
		}

		if (game.getCreator() == null || !game.getCreator().getId().equals(joueurId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le créateur peut lancer la partie");
		}

		List<GamePlayer> players = game.getPlayers();
		if (players == null || players.size() < 3) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il faut au moins 3 joueurs pour lancer la partie");
		}

		// 1. Charger et mélanger le deck complet (108 cartes)
		List<Card> deck = new ArrayList<>(cardRepository.findAll());
		Collections.shuffle(deck);

		// 2. Distribuer 7 cartes à chaque joueur dans l'ordre de tour
		players.sort(Comparator.comparingInt(GamePlayer::getTurnOrder));
		int cardIndex = 0;
		List<HandCard> allHandCards = new ArrayList<>();
		for (GamePlayer player : players) {
			for (int i = 0; i < 7; i++) {
				HandCard hc = new HandCard(player, deck.get(cardIndex++));
				player.addCard(hc);
				allHandCards.add(hc);
			}
		}
		handCardRepository.saveAll(allHandCards);

		// 3. Première carte de la défausse : ne peut pas être WILD ni WILD_DRAW_FOUR
		List<Card> remaining = new ArrayList<>(deck.subList(cardIndex, deck.size()));
		Card firstDiscard = remaining.stream()
				.filter(c -> c.getType() != CardType.WILD && c.getType() != CardType.WILD_DRAW_FOUR)
				.findFirst()
				.orElse(remaining.get(0)); // fallback (cas théoriquement impossible)
		remaining.remove(firstDiscard);

		// 4. Configurer la pioche et la défausse
		game.setDrawPile(remaining);
		List<Card> discardPile = new ArrayList<>();
		discardPile.add(firstDiscard);
		game.setDiscardPile(discardPile);
		game.setTopCard(firstDiscard);
		game.setCurrentColor(firstDiscard.getColor());

		// 5. Joueur de départ aléatoire
		game.setCurrentPlayerIndex(new Random().nextInt(players.size()));

		// 6. Lancer la partie
		game.setStatus(GameStatus.IN_PROGRESS);
		gameRepository.save(game);

		return ResponseEntity.ok(toResponse(game));
	}

	// ---- état et actions en jeu ----

	@GetMapping("/games/{gameId}/state")
	@Transactional
	public ResponseEntity<GameStateResponse> getGameState(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId) {

		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		if (game.getStatus() != GameStatus.IN_PROGRESS && game.getStatus() != GameStatus.FINISHED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La partie n'est pas en cours");
		}

		return ResponseEntity.ok(toStateResponse(game, joueurId));
	}

	@PostMapping("/games/{gameId}/play")
	@Transactional
	public ResponseEntity<GameStateResponse> playCard(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId,
			@RequestParam("cardId") Long cardId,
			@RequestParam(name = "chosenColor", required = false) CardColor chosenColor) {

		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La partie n'est pas en cours");
		}

		List<GamePlayer> sorted = sortedPlayers(game);
		GamePlayer currentGP = sorted.get(game.getCurrentPlayerIndex());

		if (!currentGP.getJoueur().getId().equals(joueurId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce n'est pas votre tour");
		}

		// Trouver la carte dans la main du joueur
		HandCard hcToPlay = currentGP.getHand().stream()
				.filter(hc -> hc.getCard().getId().equals(cardId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carte introuvable dans votre main"));

		Card card = hcToPlay.getCard();

		// Vérifier que la carte est jouable
		if (!isPlayable(card, game.getCurrentColor(), game.getTopCard())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette carte n'est pas jouable");
		}

		// WILD sans couleur choisie → erreur
		if ((card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) && chosenColor == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous devez choisir une couleur pour ce joker");
		}

		// Jouer la carte : retirer de la main, ajouter à la défausse
		currentGP.getHand().remove(hcToPlay);
		handCardRepository.delete(hcToPlay);
		game.getDiscardPile().add(card);
		game.setTopCard(card);

		// Mettre à jour la couleur active
		if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
			game.setCurrentColor(chosenColor);
		} else {
			game.setCurrentColor(card.getColor());
		}

		// Réinitialiser le flag UNO du joueur si sa main n'est pas vide
		if (!currentGP.getHand().isEmpty()) {
			currentGP.setHasCalledUno(false);
		}

		// Vérifier la victoire
		if (currentGP.getHand().isEmpty()) {
			game.setStatus(GameStatus.FINISHED);
			gameRepository.save(game);
			return ResponseEntity.ok(toStateResponse(game, joueurId));
		}

		// Appliquer les effets et passer la main
		applyCardEffect(game, sorted, card, chosenColor);

		gameRepository.save(game);
		return ResponseEntity.ok(toStateResponse(game, joueurId));
	}

	@PostMapping("/games/{gameId}/draw")
	@Transactional
	public ResponseEntity<GameStateResponse> drawCard(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId) {

		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La partie n'est pas en cours");
		}

		List<GamePlayer> sorted = sortedPlayers(game);
		GamePlayer currentGP = sorted.get(game.getCurrentPlayerIndex());

		if (!currentGP.getJoueur().getId().equals(joueurId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce n'est pas votre tour");
		}

		// Recharger la pioche si vide
		rechargePiocheIfNeeded(game);

		if (game.getDrawPile().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plus de cartes disponibles");
		}

		// Piocher la première carte
		Card drawn = game.getDrawPile().remove(0);
		HandCard hc = new HandCard(currentGP, drawn);
		currentGP.addCard(hc);
		handCardRepository.save(hc);

		// Piocher termine le tour
		game.setCurrentPlayerIndex(nextIndex(game, sorted, 0));
		currentGP.setHasCalledUno(false);

		gameRepository.save(game);
		return ResponseEntity.ok(toStateResponse(game, joueurId));
	}

	@PostMapping("/games/{gameId}/uno")
	@Transactional
	public ResponseEntity<Void> callUno(
			@PathVariable("gameId") Long gameId,
			@RequestParam("joueurId") Long joueurId) {

		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game introuvable"));

		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La partie n'est pas en cours");
		}

		GamePlayer gp = game.getPlayers().stream()
				.filter(p -> p.getJoueur().getId().equals(joueurId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "joueur non présent dans cette partie"));

		gp.setHasCalledUno(true);
		gamePlayerRepository.save(gp);

		return ResponseEntity.ok().build();
	}

	// ---- méthodes utilitaires ----

	/** Retourne les joueurs triés par turnOrder (ordre de jeu stable). */
	private List<GamePlayer> sortedPlayers(Game game) {
		return game.getPlayers().stream()
				.sorted(Comparator.comparingInt(GamePlayer::getTurnOrder))
				.collect(Collectors.toList());
	}

	/** Calcule l'index du prochain joueur en tenant compte de la direction et d'un éventuel skip. */
	private int nextIndex(Game game, List<GamePlayer> sorted, int extraSkip) {
		int n = sorted.size();
		return ((game.getCurrentPlayerIndex() + game.getDirection() * (1 + extraSkip)) % n + n) % n;
	}

	/**
	 * Vérifie si une carte est jouable.
	 * Règles UNO : même couleur active, même type d'action, même valeur numérique, ou carte WILD.
	 */
	private boolean isPlayable(Card card, CardColor currentColor, Card topCard) {
		if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
			return true;
		}
		if (card.getColor() == currentColor) {
			return true;
		}
		if (topCard != null) {
			// Même type d'action (Skip sur Skip, Reverse sur Reverse, etc.)
			if (card.getType() != CardType.NUMBER && card.getType() == topCard.getType()) {
				return true;
			}
			// Même valeur numérique
			if (card.getType() == CardType.NUMBER && topCard.getType() == CardType.NUMBER
					&& card.getValue() == topCard.getValue()) {
				return true;
			}
		}
		return false;
	}

	/** Applique l'effet de la carte jouée et avance le tour. */
	private void applyCardEffect(Game game, List<GamePlayer> sorted, Card card, CardColor chosenColor) {
		switch (card.getType()) {
			case SKIP -> {
				// Le prochain joueur passe son tour
				game.setCurrentPlayerIndex(nextIndex(game, sorted, 1));
			}
			case REVERSE -> {
				game.reverseDirection();
				if (sorted.size() == 2) {
					// À 2 joueurs, Reverse agit comme un Skip
					game.setCurrentPlayerIndex(nextIndex(game, sorted, 0));
				} else {
					game.setCurrentPlayerIndex(nextIndex(game, sorted, 0));
				}
			}
			case DRAW_TWO -> {
				// Le prochain joueur pioche 2 cartes et passe son tour
				int nextIdx = nextIndex(game, sorted, 0);
				GamePlayer nextGP = sorted.get(nextIdx);
				forceDraw(game, nextGP, 2);
				game.setCurrentPlayerIndex(nextIndex(game, sorted, 1));
			}
			case WILD -> {
				// Couleur déjà mise à jour, on avance normalement
				game.setCurrentPlayerIndex(nextIndex(game, sorted, 0));
			}
			case WILD_DRAW_FOUR -> {
				// Le prochain joueur pioche 4 cartes et passe son tour
				int nextIdx = nextIndex(game, sorted, 0);
				GamePlayer nextGP = sorted.get(nextIdx);
				forceDraw(game, nextGP, 4);
				game.setCurrentPlayerIndex(nextIndex(game, sorted, 1));
			}
			default -> {
				// Carte numéro : on avance normalement
				game.setCurrentPlayerIndex(nextIndex(game, sorted, 0));
			}
		}
	}

	/** Force un joueur à piocher {@code count} cartes (effets +2 / +4). */
	private void forceDraw(Game game, GamePlayer target, int count) {
		for (int i = 0; i < count; i++) {
			rechargePiocheIfNeeded(game);
			if (game.getDrawPile().isEmpty()) break;
			Card drawn = game.getDrawPile().remove(0);
			HandCard hc = new HandCard(target, drawn);
			target.addCard(hc);
			handCardRepository.save(hc);
		}
	}

	/** Remet la défausse (sauf la topCard) dans la pioche si celle-ci est vide. */
	private void rechargePiocheIfNeeded(Game game) {
		if (!game.getDrawPile().isEmpty()) return;
		List<Card> discard = game.getDiscardPile();
		if (discard.size() <= 1) return; // seul la topCard reste, rien à faire

		// Garder uniquement la topCard dans la défausse
		Card top = game.getTopCard();
		List<Card> toReshuffle = new ArrayList<>(discard.subList(0, discard.size() - 1));
		discard.clear();
		if (top != null) discard.add(top);

		Collections.shuffle(toReshuffle);
		game.setDrawPile(toReshuffle);
	}

	/** Construit le GameStateResponse vu par un joueur donné. */
	private GameStateResponse toStateResponse(Game game, Long joueurId) {
		List<GamePlayer> sorted = sortedPlayers(game);

		GamePlayer me = sorted.stream()
				.filter(gp -> gp.getJoueur().getId().equals(joueurId))
				.findFirst()
				.orElse(null);

		GamePlayer currentGP = sorted.isEmpty() ? null : sorted.get(game.getCurrentPlayerIndex());

		List<PlayerStateDto> playerStates = new ArrayList<>();
		for (int i = 0; i < sorted.size(); i++) {
			GamePlayer gp = sorted.get(i);
			playerStates.add(new PlayerStateDto(
					gp.getJoueur().getId(),
					gp.getJoueur().getPseudo(),
					gp.getHand().size(),
					i == game.getCurrentPlayerIndex(),
					gp.isHasCalledUno()));
		}

		List<CardDto> myHand = (me == null) ? new ArrayList<>() :
				me.getHand().stream().map(hc -> new CardDto(hc.getCard())).collect(Collectors.toList());

		// Chercher le vainqueur (joueur avec 0 carte si partie FINISHED)
		String winnerPseudo = null;
		if (game.getStatus() == GameStatus.FINISHED) {
			winnerPseudo = sorted.stream()
					.filter(gp -> gp.getHand().isEmpty())
					.map(gp -> gp.getJoueur().getPseudo())
					.findFirst()
					.orElse("Inconnu");
		}

		GameStateResponse resp = new GameStateResponse();
		resp.setGameId(game.getId());
		resp.setStatus(game.getStatus());
		resp.setTopCard(game.getTopCard() != null ? new CardDto(game.getTopCard()) : null);
		resp.setCurrentColor(game.getCurrentColor());
		resp.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
		resp.setCurrentPlayerPseudo(currentGP != null ? currentGP.getJoueur().getPseudo() : null);
		resp.setMyTurn(me != null && currentGP != null && me.getId().equals(currentGP.getId()));
		resp.setDirection(game.getDirection());
		resp.setMyHand(myHand);
		resp.setPlayers(playerStates);
		resp.setWinnerPseudo(winnerPseudo);
		return resp;
	}

	private GameResponse toResponse(Game game) {
		int playerCount = (game.getPlayers() == null) ? 0 : game.getPlayers().size();
		Long creatorId = (game.getCreator() != null) ? game.getCreator().getId() : null;
		List<String> playerPseudos = (game.getPlayers() == null) ? java.util.Collections.emptyList() :
				game.getPlayers().stream().map(gp -> gp.getJoueur().getPseudo()).collect(Collectors.toList());
		return new GameResponse(game.getId(), game.getStatus(), game.getCreatedAt(), playerCount, game.getName(), creatorId, playerPseudos);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

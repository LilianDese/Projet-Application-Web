package n7.facade.web;

import java.util.List;
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

import n7.facade.model.Game;
import n7.facade.model.GamePlayer;
import n7.facade.model.GameStatus;
import n7.facade.model.Joueur;
import n7.facade.repository.GamePlayerRepository;
import n7.facade.repository.GameRepository;
import n7.facade.repository.JoueurRepository;
import n7.facade.web.dto.CreateGameRequest;
import n7.facade.web.dto.FacadeJoueurRequest;
import n7.facade.web.dto.FacadeJoueurResponse;
import n7.facade.web.dto.GameResponse;
import n7.facade.web.dto.JoinGameResponse;

/**
 * Facade REST 
 */
@RestController
public class FacadeController {
    
	private final JoueurRepository joueurRepository;
	private final GameRepository gameRepository;
	private final GamePlayerRepository gamePlayerRepository;

	public FacadeController(
			JoueurRepository joueurRepository,
			GameRepository gameRepository,
			GamePlayerRepository gamePlayerRepository) {
		this.joueurRepository = joueurRepository;
		this.gameRepository = gameRepository;
		this.gamePlayerRepository = gamePlayerRepository;
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

		if (game.getPlayers() == null || game.getPlayers().size() < 3) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il faut au moins 3 joueurs pour lancer la partie");
		}

		game.setStatus(GameStatus.IN_PROGRESS);
		gameRepository.save(game);

		return ResponseEntity.ok(toResponse(game));
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

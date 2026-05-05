package n7.facade.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import n7.facade.model.Game;
import n7.facade.model.GamePlayer;
import n7.facade.model.GameStatus;
import n7.facade.repository.GamePlayerRepository;
import n7.facade.repository.GameRepository;
import n7.facade.web.dto.CardDto;
import n7.facade.web.dto.GameResponse;
import n7.facade.web.dto.GameStateResponse;
import n7.facade.web.dto.PlayerStateDto;

@Service
public class PlayerDisconnectService {

	private final GamePlayerRepository gamePlayerRepository;
	private final GameRepository gameRepository;
	private final SimpMessagingTemplate messaging;

	public PlayerDisconnectService(
			GamePlayerRepository gamePlayerRepository,
			GameRepository gameRepository,
			@Lazy SimpMessagingTemplate messaging) {
		this.gamePlayerRepository = gamePlayerRepository;
		this.gameRepository = gameRepository;
		this.messaging = messaging;
	}

	@Transactional
	public void removeFromAllGames(Long joueurId) {
		for (GamePlayer gp : gamePlayerRepository.findAllByJoueurId(joueurId)) {
			Game game = gp.getGame();
			if (game == null) continue;
			if (game.getStatus() == GameStatus.LOBBY) {
				removeFromLobby(game, joueurId);
			} else if (game.getStatus() == GameStatus.IN_PROGRESS) {
				removeFromInProgress(game, joueurId);
			}
		}
	}

	private void removeFromLobby(Game game, Long joueurId) {
		var gps = gamePlayerRepository.findAllByGameIdAndJoueurId(game.getId(), joueurId);
		if (gps.isEmpty()) return;

		if (game.getPlayers() != null)
			game.getPlayers().removeIf(p -> p.getJoueur() != null && joueurId.equals(p.getJoueur().getId()));
		gamePlayerRepository.deleteAll(gps);

		if (game.getCreator() != null && joueurId.equals(game.getCreator().getId())) {
			game.setCreator(game.getPlayers() != null && !game.getPlayers().isEmpty()
					? game.getPlayers().get(0).getJoueur() : null);
		}

		if (gamePlayerRepository.countByGameId(game.getId()) <= 0) {
			gameRepository.delete(game);
		} else {
			gameRepository.save(game);
			broadcastLobby(game);
		}
		broadcastGamesList();
	}

	private void removeFromInProgress(Game game, Long joueurId) {
		List<GamePlayer> sorted = sortedPlayers(game);

		int leavingIndex = -1;
		GamePlayer leavingGP = null;
		for (int i = 0; i < sorted.size(); i++) {
			if (sorted.get(i).getJoueur().getId().equals(joueurId)) {
				leavingIndex = i;
				leavingGP = sorted.get(i);
				break;
			}
		}
		if (leavingIndex == -1) return;

		game.getPlayers().removeIf(p -> p.getJoueur() != null && joueurId.equals(p.getJoueur().getId()));
		gamePlayerRepository.delete(leavingGP);

		List<GamePlayer> remaining = sortedPlayers(game);

		if (remaining.isEmpty()) {
			gameRepository.delete(game);
			broadcastGamesList();
			return;
		}

		// Réassigner les turnOrder pour qu'ils soient contigus (0, 1, 2 ...)
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setTurnOrder(i);
			gamePlayerRepository.save(remaining.get(i));
		}

		// Ajuster currentPlayerIndex après le retrait
		int idx = game.getCurrentPlayerIndex();
		if (leavingIndex < idx) {
			idx -= 1;
		} else if (leavingIndex == idx) {
			idx = idx % remaining.size();
		}
		game.setCurrentPlayerIndex(idx);

		if (remaining.size() < 2) {
			game.setStatus(GameStatus.FINISHED);
		}

		gameRepository.save(game);
		broadcastGameState(game, remaining);
		broadcastGamesList();
	}

	// -------------------------------------------------------
	// Helpers broadcast
	// -------------------------------------------------------

	private void broadcastGamesList() {
		List<GameResponse> games = gameRepository.findAll().stream()
				.map(this::toGameResponse)
				.collect(Collectors.toList());
		messaging.convertAndSend("/topic/games", games);
	}

	private void broadcastLobby(Game game) {
		messaging.convertAndSend("/topic/game/" + game.getId() + "/lobby", toGameResponse(game));
	}

	private void broadcastGameState(Game game, List<GamePlayer> sorted) {
		for (GamePlayer gp : sorted) {
			Long jid = gp.getJoueur().getId();
			messaging.convertAndSend(
					"/topic/game/" + game.getId() + "/player/" + jid,
					toGameStateResponse(game, sorted, jid));
		}
	}

	// -------------------------------------------------------
	// Conversions DTO
	// -------------------------------------------------------

	private GameResponse toGameResponse(Game game) {
		int count = game.getPlayers() == null ? 0 : game.getPlayers().size();
		Long creatorId = game.getCreator() != null ? game.getCreator().getId() : null;
		List<String> pseudos = game.getPlayers() == null ? java.util.Collections.emptyList()
				: game.getPlayers().stream().map(gp -> gp.getJoueur().getPseudo()).collect(Collectors.toList());
		return new GameResponse(game.getId(), game.getStatus(), game.getCreatedAt(), count, game.getName(), creatorId, pseudos);
	}

	private GameStateResponse toGameStateResponse(Game game, List<GamePlayer> sorted, Long joueurId) {
		GamePlayer me = sorted.stream()
				.filter(gp -> gp.getJoueur().getId().equals(joueurId))
				.findFirst().orElse(null);
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

		List<CardDto> myHand = me == null ? new ArrayList<>()
				: me.getHand().stream().map(hc -> new CardDto(hc.getCard())).collect(Collectors.toList());

		// Gagnant : soit celui qui n'a plus de cartes, soit le dernier restant (forfait)
		String winnerPseudo = null;
		if (game.getStatus() == GameStatus.FINISHED) {
			winnerPseudo = sorted.stream()
					.filter(gp -> gp.getHand().isEmpty())
					.map(gp -> gp.getJoueur().getPseudo())
					.findFirst()
					.orElse(sorted.isEmpty() ? "Inconnu" : sorted.get(0).getJoueur().getPseudo());
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

	private List<GamePlayer> sortedPlayers(Game game) {
		return game.getPlayers().stream()
				.sorted(Comparator.comparingInt(GamePlayer::getTurnOrder))
				.collect(Collectors.toList());
	}
}

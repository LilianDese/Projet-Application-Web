package n7.facade.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import n7.facade.repository.JoueurRepository;
import n7.facade.service.PlayerDisconnectService;

@Component
public class WebSocketPresenceTracker {

	private final JoueurRepository joueurRepository;
	private final PlayerDisconnectService playerDisconnectService;
	private final ConcurrentMap<String, Long> sessionToJoueur = new ConcurrentHashMap<>();
	private final ConcurrentMap<Long, Integer> joueurSessions = new ConcurrentHashMap<>();

	public WebSocketPresenceTracker(JoueurRepository joueurRepository, PlayerDisconnectService playerDisconnectService) {
		this.joueurRepository = joueurRepository;
		this.playerDisconnectService = playerDisconnectService;
	}

	public void registerSession(String sessionId, Long joueurId) {
		if (sessionId == null || joueurId == null) return;

		Long existing = sessionToJoueur.put(sessionId, joueurId);
		if (existing != null) return;

		int count = joueurSessions.merge(joueurId, 1, Integer::sum);
		if (count == 1) {
			setConnected(joueurId, true);
		}
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		String sessionId = event.getSessionId();
		Long joueurId = sessionToJoueur.remove(sessionId);
		if (joueurId == null) return;

		AtomicBoolean lastSession = new AtomicBoolean(false);
		joueurSessions.compute(joueurId, (id, current) -> {
			if (current == null || current <= 1) {
				lastSession.set(true);
				return null;
			}
			return current - 1;
		});

		if (lastSession.get()) {
			playerDisconnectService.removeFromAllGames(joueurId);
			setConnected(joueurId, false);
		}
	}

	private void setConnected(Long joueurId, boolean connected) {
		joueurRepository.findById(joueurId).ifPresent(joueur -> {
			if (joueur.isConnected() != connected) {
				joueur.setConnected(connected);
				joueurRepository.save(joueur);
			}
		});
	}
}

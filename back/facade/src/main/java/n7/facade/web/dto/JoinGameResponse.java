package n7.facade.web.dto;

public class JoinGameResponse {
	private Long participationId;
	private Long gameId;
	private Long joueurId;
	private int turnOrder;

	public JoinGameResponse() {
	}

	public JoinGameResponse(Long participationId, Long gameId, Long joueurId, int turnOrder) {
		this.participationId = participationId;
		this.gameId = gameId;
		this.joueurId = joueurId;
		this.turnOrder = turnOrder;
	}

	public Long getParticipationId() {
		return participationId;
	}

	public void setParticipationId(Long participationId) {
		this.participationId = participationId;
	}

	public Long getGameId() {
		return gameId;
	}

	public void setGameId(Long gameId) {
		this.gameId = gameId;
	}

	public Long getJoueurId() {
		return joueurId;
	}

	public void setJoueurId(Long joueurId) {
		this.joueurId = joueurId;
	}

	public int getTurnOrder() {
		return turnOrder;
	}

	public void setTurnOrder(int turnOrder) {
		this.turnOrder = turnOrder;
	}
}

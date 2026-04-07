package n7.facade.web.dto;

import java.time.LocalDateTime;

import n7.facade.model.GameStatus;

public class GameResponse {
	private Long id;
	private GameStatus status;
	private LocalDateTime createdAt;
	private int playerCount;
	private String name;

	public GameResponse() {
	}

	public GameResponse(Long id, GameStatus status, LocalDateTime createdAt, int playerCount, String name) {
		this.id = id;
		this.status = status;
		this.createdAt = createdAt;
		this.playerCount = playerCount;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GameStatus getStatus() {
		return status;
	}

	public void setStatus(GameStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}
}

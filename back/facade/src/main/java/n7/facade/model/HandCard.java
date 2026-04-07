package n7.facade.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Représente une carte dans la main d'un joueur pendant une partie.
 * C'est la table d'association entre GamePlayer et Card.
 *
 * Relations :
 * - ManyToOne vers GamePlayer (porteur de la relation — FK game_player_id)
 * - ManyToOne vers Card (porteur de la relation — FK card_id)
 */
@Entity
@Table(name = "hand_card")
public class HandCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "game_player_id")
	private GamePlayer gamePlayer;

	@ManyToOne
	@JoinColumn(name = "card_id")
	private Card card;

	protected HandCard() {
		// JPA
	}

	public HandCard(GamePlayer gamePlayer, Card card) {
		this.gamePlayer = gamePlayer;
		this.card = card;
	}

	// Getters / setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GamePlayer getGamePlayer() {
		return gamePlayer;
	}

	public void setGamePlayer(GamePlayer gamePlayer) {
		this.gamePlayer = gamePlayer;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}
}

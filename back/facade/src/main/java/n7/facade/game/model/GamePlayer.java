package n7.facade.game.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import n7.facade.joueur.model.Joueur;

/**
 * Représente la participation d'un joueur à une partie.
 * C'est la table d'association entre Game et Joueur, enrichie
 * de champs supplémentaires (ordre de tour, UNO appelé, etc.).
 *
 * Relations :
 * - ManyToOne vers Game (porteur de la relation — côté FK game_id)
 * - ManyToOne vers Joueur (porteur de la relation — côté FK joueur_id)
 * - OneToMany bidirectionnelle vers HandCard (cartes en main)
 */
@Entity
@Table(name = "game_player")
public class GamePlayer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Ordre de jeu dans la partie (0, 1, 2, …) */
	private int turnOrder;

	/** Le joueur a-t-il crié UNO ? */
	private boolean hasCalledUno;

	// ---- relations ----

	@ManyToOne
	@JoinColumn(name = "game_id")
	private Game game;

	@ManyToOne
	@JoinColumn(name = "joueur_id")
	private Joueur joueur;

	/** Cartes en main du joueur pour cette partie */
	@OneToMany(mappedBy = "gamePlayer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<HandCard> hand;

	protected GamePlayer() {
		// JPA
	}

	public GamePlayer(Game game, Joueur joueur, int turnOrder) {
		this.game = game;
		this.joueur = joueur;
		this.turnOrder = turnOrder;
		this.hasCalledUno = false;
		this.hand = new ArrayList<>();
	}

	// Getters / setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getTurnOrder() {
		return turnOrder;
	}

	public void setTurnOrder(int turnOrder) {
		this.turnOrder = turnOrder;
	}

	public boolean isHasCalledUno() {
		return hasCalledUno;
	}

	public void setHasCalledUno(boolean hasCalledUno) {
		this.hasCalledUno = hasCalledUno;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Joueur getJoueur() {
		return joueur;
	}

	public void setJoueur(Joueur joueur) {
		this.joueur = joueur;
	}

	public List<HandCard> getHand() {
		return hand;
	}

	public void setHand(List<HandCard> hand) {
		this.hand = hand;
	}

	// utilitaires

	public void addCard(HandCard handCard) {
		this.hand.add(handCard);
		handCard.setGamePlayer(this);
	}

	public void removeCard(HandCard handCard) {
		this.hand.remove(handCard);
		handCard.setGamePlayer(null);
	}
}

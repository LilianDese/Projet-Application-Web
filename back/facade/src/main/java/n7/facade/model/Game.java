package n7.facade.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Représente une partie de UNO.
 * Contient l'état du jeu, les joueurs, la pioche, la défausse, le chat et les
 * règles.
 *
 * Relations :
 * - OneToMany bidirectionnelle vers GamePlayer (joueurs de la partie)
 * - OneToMany bidirectionnelle vers ChatMessage (messages du chat)
 * - OneToMany bidirectionnelle vers GameRule (règles optionnelles)
 * - ManyToMany ordonnée vers Card (pioche = drawPile)
 * - ManyToMany ordonnée vers Card (défausse = discardPile)
 * - ManyToOne vers Card (carte du dessus de la défausse, pour accès rapide)
 */
@Entity
@Table(name = "game")
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GameStatus status;

	/** +1 sens horaire, -1 sens anti-horaire */
	private int direction;

	/** Index du joueur dont c'est le tour dans la liste players */
	private int currentPlayerIndex;

	/** Couleur active (peut différer de topCard.color après un WILD joué) */
	@Enumerated(EnumType.STRING)
	private CardColor currentColor;

	@Column(nullable = false)
	private String name;

	private LocalDateTime createdAt;

	// ---- relations ----

	/** Le créateur de la partie (qui peut la lancer) */
	@ManyToOne
	@JoinColumn(name = "creator_id")
	private Joueur creator;

	/** Joueurs participant à la partie (bidirectionnelle, mappedBy côté Game) */
	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<GamePlayer> players;

	/** Messages du chat de la partie */
	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ChatMessage> chatMessages;

	/** Règles optionnelles activées */
	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<GameRule> rules;

	/** Pile de pioche (ordonnée) */
	@ManyToMany
	@JoinTable(name = "game_draw_pile", joinColumns = @JoinColumn(name = "game_id"), inverseJoinColumns = @JoinColumn(name = "card_id"))
	@OrderColumn(name = "pile_order")
	private List<Card> drawPile;

	/** Pile de défausse (ordonnée, la dernière carte est le dessus) */
	@ManyToMany
	@JoinTable(name = "game_discard_pile", joinColumns = @JoinColumn(name = "game_id"), inverseJoinColumns = @JoinColumn(name = "card_id"))
	@OrderColumn(name = "pile_order")
	private List<Card> discardPile;

	/** Carte actuellement sur le dessus de la défausse (accès rapide) */
	@ManyToOne
	@JoinColumn(name = "top_card_id")
	private Card topCard;

	protected Game() {
		// constructeur vide requis par JPA
	}

	public Game(GameStatus status) {
		this(status, "Partie sans nom");
	}

	public Game(GameStatus status, String name) {
		this.status = status;
		this.direction = 1;
		this.currentPlayerIndex = 0;
		this.name = name;
		this.createdAt = LocalDateTime.now();
		this.players = new ArrayList<>();
		this.chatMessages = new ArrayList<>();
		this.rules = new ArrayList<>();
		this.drawPile = new ArrayList<>();
		this.discardPile = new ArrayList<>();
	}

	// Getters / setters

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

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getCurrentPlayerIndex() {
		return currentPlayerIndex;
	}

	public void setCurrentPlayerIndex(int currentPlayerIndex) {
		this.currentPlayerIndex = currentPlayerIndex;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GamePlayer> getPlayers() {
		return players;
	}

	public void setPlayers(List<GamePlayer> players) {
		this.players = players;
	}

	public List<ChatMessage> getChatMessages() {
		return chatMessages;
	}

	public void setChatMessages(List<ChatMessage> chatMessages) {
		this.chatMessages = chatMessages;
	}

	public List<GameRule> getRules() {
		return rules;
	}

	public void setRules(List<GameRule> rules) {
		this.rules = rules;
	}

	public List<Card> getDrawPile() {
		return drawPile;
	}

	public void setDrawPile(List<Card> drawPile) {
		this.drawPile = drawPile;
	}

	public List<Card> getDiscardPile() {
		return discardPile;
	}

	public void setDiscardPile(List<Card> discardPile) {
		this.discardPile = discardPile;
	}

	public Card getTopCard() {
		return topCard;
	}

	public void setTopCard(Card topCard) {
		this.topCard = topCard;
	}

	public CardColor getCurrentColor() {
		return currentColor;
	}

	public void setCurrentColor(CardColor currentColor) {
		this.currentColor = currentColor;
	}

	public Joueur getCreator() {
		return creator;
	}

	public void setCreator(Joueur creator) {
		this.creator = creator;
	}

	// utilitaires

	public void addPlayer(GamePlayer player) {
		this.players.add(player);
		player.setGame(this);
	}

	public void addChatMessage(ChatMessage message) {
		this.chatMessages.add(message);
		message.setGame(this);
	}

	public void addRule(GameRule rule) {
		this.rules.add(rule);
		rule.setGame(this);
	}

	public void reverseDirection() {
		this.direction *= -1;
	}
}

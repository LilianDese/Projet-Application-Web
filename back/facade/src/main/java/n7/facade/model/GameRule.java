package n7.facade.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Représente une règle optionnelle activée pour une partie.
 * Exemples : "piocher jusqu'à pouvoir jouer", "empilage +2", etc.
 *
 * Relation :
 * - ManyToOne vers Game (porteur de la relation — FK game_id)
 */
@Entity
@Table(name = "game_rule")
public class GameRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String ruleName;

	private String description;

	// ---- relation ----

	@ManyToOne
	@JoinColumn(name = "game_id")
	private Game game;

	protected GameRule() {
		// JPA
	}

	public GameRule(String ruleName, String description) {
		this.ruleName = ruleName;
		this.description = description;
	}

	// Getters / setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}
}

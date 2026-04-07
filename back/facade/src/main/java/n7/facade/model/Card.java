package n7.facade.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Représente une carte du jeu UNO.
 * Les 108 cartes sont créées une seule fois au démarrage de l'application.
 * Chaque carte a une couleur, un type et une valeur (0-9 pour les numéros).
 */
@Entity
@Table(name = "card")
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CardColor color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CardType type;

	/** Valeur numérique (0-9) pour les cartes NUMBER, -1 sinon. */
	private int value;

	protected Card() {
		// JPA
	}

	public Card(CardColor color, CardType type, int value) {
		this.color = color;
		this.type = type;
		this.value = value;
	}

	// Getters / setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CardColor getColor() {
		return color;
	}

	public void setColor(CardColor color) {
		this.color = color;
	}

	public CardType getType() {
		return type;
	}

	public void setType(CardType type) {
		this.type = type;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if (type == CardType.NUMBER) {
			return color + " " + value;
		}
		return color + " " + type;
	}
}

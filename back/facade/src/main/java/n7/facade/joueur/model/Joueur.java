package n7.facade.joueur.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "joueur")
public class Joueur {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String pseudo;

	protected Joueur() {
		// JPA
	}

	public Joueur(String pseudo) {
		this.pseudo = pseudo;
	}

	public Long getId() {
		return id;
	}

	public String getPseudo() {
		return pseudo;
	}
}

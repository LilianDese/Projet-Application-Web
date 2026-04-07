package n7.facade.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "joueur")
public class Joueur {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String pseudo;

	private String password;

	private int wins;

	private int gamesPlayed;

	/** Participations de ce joueur aux différentes parties */
	@OneToMany(mappedBy = "joueur", fetch = FetchType.LAZY)
	private List<GamePlayer> participations;

	protected Joueur() {
		// JPA
	}

	public Joueur(String pseudo) {
		this.pseudo = pseudo;
		this.wins = 0;
		this.gamesPlayed = 0;
		this.participations = new ArrayList<>();
	}

	public Joueur(String pseudo, String password) {
		this(pseudo);
		this.password = password;
	}

	// Getters / setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPseudo() {
		return pseudo;
	}

	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getWins() {
		return wins;
	}

	public void setWins(int wins) {
		this.wins = wins;
	}

	public int getGamesPlayed() {
		return gamesPlayed;
	}

	public void setGamesPlayed(int gamesPlayed) {
		this.gamesPlayed = gamesPlayed;
	}

	public List<GamePlayer> getParticipations() {
		return participations;
	}

	public void setParticipations(List<GamePlayer> participations) {
		this.participations = participations;
	}
}

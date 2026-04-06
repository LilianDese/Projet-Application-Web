package n7.facade.config;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import n7.facade.game.model.Card;
import n7.facade.game.model.CardColor;
import n7.facade.game.model.CardType;
import n7.facade.game.repository.CardRepository;
import n7.facade.joueur.model.Joueur;
import n7.facade.joueur.repository.JoueurRepository;

/**
 * Initialise la base de données au démarrage de l'application.
 * Annotée @PostConstruct comme vu en cours pour peupler la BD avec des données de test.
 *
 * Crée les 108 cartes UNO standard :
 * - 4 couleurs × ( 1×0 + 2×(1-9) + 2×Skip + 2×Reverse + 2×DrawTwo ) = 100 cartes couleur
 * - 4 Wild + 4 Wild Draw Four = 8 cartes sauvages
 * Total = 108 cartes
 *
 * Crée aussi quelques joueurs de test pour faciliter le debug.
 */
@Component
public class DataInitializer {

	private final CardRepository cardRepository;
	private final JoueurRepository joueurRepository;

	public DataInitializer(CardRepository cardRepository, JoueurRepository joueurRepository) {
		this.cardRepository = cardRepository;
		this.joueurRepository = joueurRepository;
	}

	@PostConstruct
	public void init() {
		// Ne pas re-peupler si les cartes existent déjà (cas ddl-auto=update)
		if (cardRepository.count() > 0) {
			return;
		}

		createCards();
		createTestPlayers();
	}

	/**
	 * Crée le jeu complet de 108 cartes UNO.
	 */
	private void createCards() {
		CardColor[] colors = { CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW };

		for (CardColor color : colors) {
			// 1 carte "0" par couleur
			cardRepository.save(new Card(color, CardType.NUMBER, 0));

			// 2 cartes de chaque chiffre 1-9 par couleur
			for (int value = 1; value <= 9; value++) {
				cardRepository.save(new Card(color, CardType.NUMBER, value));
				cardRepository.save(new Card(color, CardType.NUMBER, value));
			}

			// 2 cartes Skip par couleur
			cardRepository.save(new Card(color, CardType.SKIP, -1));
			cardRepository.save(new Card(color, CardType.SKIP, -1));

			// 2 cartes Reverse par couleur
			cardRepository.save(new Card(color, CardType.REVERSE, -1));
			cardRepository.save(new Card(color, CardType.REVERSE, -1));

			// 2 cartes Draw Two (+2) par couleur
			cardRepository.save(new Card(color, CardType.DRAW_TWO, -1));
			cardRepository.save(new Card(color, CardType.DRAW_TWO, -1));
		}

		// 4 cartes Wild (joker changement de couleur)
		for (int i = 0; i < 4; i++) {
			cardRepository.save(new Card(CardColor.WILD, CardType.WILD, -1));
		}

		// 4 cartes Wild Draw Four (+4)
		for (int i = 0; i < 4; i++) {
			cardRepository.save(new Card(CardColor.WILD, CardType.WILD_DRAW_FOUR, -1));
		}

		System.out.println("=== " + cardRepository.count() + " cartes UNO créées ===");
	}

	/**
	 * Crée quelques joueurs de test pour faciliter le debug.
	 */
	private void createTestPlayers() {
		if (joueurRepository.count() > 0) {
			return;
		}

		joueurRepository.save(new Joueur("Alice", "pass123"));
		joueurRepository.save(new Joueur("Bob", "pass123"));
		joueurRepository.save(new Joueur("Charlie", "pass123"));

		System.out.println("=== " + joueurRepository.count() + " joueurs de test créés ===");
	}
}

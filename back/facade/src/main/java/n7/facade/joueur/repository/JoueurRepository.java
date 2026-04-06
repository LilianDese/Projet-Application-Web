package n7.facade.joueur.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.joueur.model.Joueur;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {
	boolean existsByPseudo(String pseudo);
}

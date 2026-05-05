package n7.facade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.Joueur;

import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {
	boolean existsByPseudo(String pseudo);
	Optional<Joueur> findByPseudoIgnoreCase(String pseudo);
}

package n7.facade.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.GamePlayer;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
	List<GamePlayer> findAllByGameIdAndJoueurId(Long gameId, Long joueurId);
	Optional<GamePlayer> findTopByGameIdAndJoueurIdOrderByIdAsc(Long gameId, Long joueurId);
	long countByGameId(Long gameId);
}

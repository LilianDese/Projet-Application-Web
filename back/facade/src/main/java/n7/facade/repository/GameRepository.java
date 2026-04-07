package n7.facade.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.Game;
import n7.facade.model.GameStatus;

public interface GameRepository extends JpaRepository<Game, Long> {
	List<Game> findByStatus(GameStatus status);
}

package n7.facade.game.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.game.model.Game;
import n7.facade.game.model.GameStatus;

public interface GameRepository extends JpaRepository<Game, Long> {
	List<Game> findByStatus(GameStatus status);
}

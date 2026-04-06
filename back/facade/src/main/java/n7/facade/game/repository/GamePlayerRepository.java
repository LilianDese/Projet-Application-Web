package n7.facade.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.game.model.GamePlayer;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
}

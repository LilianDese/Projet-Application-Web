package n7.facade.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.game.model.GameRule;

public interface GameRuleRepository extends JpaRepository<GameRule, Long> {
}

package n7.facade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.GameRule;

public interface GameRuleRepository extends JpaRepository<GameRule, Long> {
}

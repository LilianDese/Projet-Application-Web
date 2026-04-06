package n7.facade.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.game.model.HandCard;

public interface HandCardRepository extends JpaRepository<HandCard, Long> {
}

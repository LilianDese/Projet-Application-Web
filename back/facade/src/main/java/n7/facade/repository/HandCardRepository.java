package n7.facade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.HandCard;

public interface HandCardRepository extends JpaRepository<HandCard, Long> {
}

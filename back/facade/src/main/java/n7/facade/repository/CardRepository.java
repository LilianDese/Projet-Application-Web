package n7.facade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
}

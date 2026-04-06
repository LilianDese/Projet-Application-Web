package n7.facade.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.game.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}

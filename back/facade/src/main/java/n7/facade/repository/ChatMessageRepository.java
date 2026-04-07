package n7.facade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import n7.facade.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}

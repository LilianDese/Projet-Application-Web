package n7.facade.web.dto;

import java.time.LocalDateTime;

public class ChatMessageResponse {
	private Long id;
	private String pseudo;
	private String content;
	private LocalDateTime sentAt;

	public ChatMessageResponse() {
	}

	public ChatMessageResponse(Long id, String pseudo, String content, LocalDateTime sentAt) {
		this.id = id;
		this.pseudo = pseudo;
		this.content = content;
		this.sentAt = sentAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPseudo() {
		return pseudo;
	}

	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}
}

package n7.facade.web.dto;

public class ChatMessageRequest {
	private Long joueurId;
	private String content;

	public ChatMessageRequest() {
	}

	public Long getJoueurId() {
		return joueurId;
	}

	public void setJoueurId(Long joueurId) {
		this.joueurId = joueurId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}

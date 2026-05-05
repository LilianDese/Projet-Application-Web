package n7.facade.web.dto;

public class FacadeJoueurResponse {
	private Long id;
	private String pseudo;
	private boolean connected;

	public FacadeJoueurResponse() {
	}

	public FacadeJoueurResponse(Long id, String pseudo) {
		this.id = id;
		this.pseudo = pseudo;
	}

	public FacadeJoueurResponse(Long id, String pseudo, boolean connected) {
		this.id = id;
		this.pseudo = pseudo;
		this.connected = connected;
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

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}
}

package n7.facade.web.dto;

public class PlayerStateDto {
    private Long joueurId;
    private String pseudo;
    private int handSize;
    private boolean currentPlayer;
    private boolean hasCalledUno;

    public PlayerStateDto() {}

    public PlayerStateDto(Long joueurId, String pseudo, int handSize, boolean currentPlayer, boolean hasCalledUno) {
        this.joueurId = joueurId;
        this.pseudo = pseudo;
        this.handSize = handSize;
        this.currentPlayer = currentPlayer;
        this.hasCalledUno = hasCalledUno;
    }

    public Long getJoueurId() { return joueurId; }
    public void setJoueurId(Long joueurId) { this.joueurId = joueurId; }

    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }

    public int getHandSize() { return handSize; }
    public void setHandSize(int handSize) { this.handSize = handSize; }

    public boolean isCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(boolean currentPlayer) { this.currentPlayer = currentPlayer; }

    public boolean isHasCalledUno() { return hasCalledUno; }
    public void setHasCalledUno(boolean hasCalledUno) { this.hasCalledUno = hasCalledUno; }
}

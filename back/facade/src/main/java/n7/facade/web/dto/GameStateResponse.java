package n7.facade.web.dto;

import java.util.List;

import n7.facade.model.CardColor;
import n7.facade.model.GameStatus;

public class GameStateResponse {
    private Long gameId;
    private GameStatus status;
    /** Carte visible du dessus de la défausse */
    private CardDto topCard;
    /** Couleur active (peut différer de topCard.color après un WILD) */
    private CardColor currentColor;
    /** Index dans players du joueur dont c'est le tour */
    private int currentPlayerIndex;
    private String currentPlayerPseudo;
    /** Est-ce le tour du joueur qui a fait la requête ? */
    private boolean myTurn;
    /** +1 sens horaire, -1 sens anti-horaire */
    private int direction;
    /** Cartes en main du joueur demandeur */
    private List<CardDto> myHand;
    /** État public de tous les joueurs (taille de main, etc.) */
    private List<PlayerStateDto> players;
    /** Pseudo du vainqueur, null si la partie est en cours */
    private String winnerPseudo;

    public GameStateResponse() {}

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public CardDto getTopCard() { return topCard; }
    public void setTopCard(CardDto topCard) { this.topCard = topCard; }

    public CardColor getCurrentColor() { return currentColor; }
    public void setCurrentColor(CardColor currentColor) { this.currentColor = currentColor; }

    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }

    public String getCurrentPlayerPseudo() { return currentPlayerPseudo; }
    public void setCurrentPlayerPseudo(String currentPlayerPseudo) { this.currentPlayerPseudo = currentPlayerPseudo; }

    public boolean isMyTurn() { return myTurn; }
    public void setMyTurn(boolean myTurn) { this.myTurn = myTurn; }

    public int getDirection() { return direction; }
    public void setDirection(int direction) { this.direction = direction; }

    public List<CardDto> getMyHand() { return myHand; }
    public void setMyHand(List<CardDto> myHand) { this.myHand = myHand; }

    public List<PlayerStateDto> getPlayers() { return players; }
    public void setPlayers(List<PlayerStateDto> players) { this.players = players; }

    public String getWinnerPseudo() { return winnerPseudo; }
    public void setWinnerPseudo(String winnerPseudo) { this.winnerPseudo = winnerPseudo; }
}

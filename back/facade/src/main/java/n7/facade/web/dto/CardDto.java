package n7.facade.web.dto;

import n7.facade.model.Card;
import n7.facade.model.CardColor;
import n7.facade.model.CardType;

public class CardDto {
    private Long id;
    private CardColor color;
    private CardType type;
    private int value;

    public CardDto() {}

    public CardDto(Card card) {
        this.id = card.getId();
        this.color = card.getColor();
        this.type = card.getType();
        this.value = card.getValue();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CardColor getColor() { return color; }
    public void setColor(CardColor color) { this.color = color; }

    public CardType getType() { return type; }
    public void setType(CardType type) { this.type = type; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}

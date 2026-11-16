package messages;

import pitiupi.models.Messages.Message;

public class PichessActionMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String playerName;
    private int playerColor;
    private long timeControl;
    private int increment;
    
    public enum PichessActions {
        INVITE,
        ACCEPT_INVITE,
        DECLINE_INVITE,
        RESIGN,
        OFFER_DRAW,
        ACCEPT_DRAW,
        DECLINE_DRAW,
        OFFER_REMATCH,
        ACCEPT_REMATCH,
        DECLINE_REMATCH
    }
    private PichessActions action;

    public PichessActionMessage() {

    }

    public PichessActionMessage(PichessActions action, String playerName, int playerColor, long timeControl, int increment) {
        this.action = action;
        this.playerName = playerName;
        this.playerColor = playerColor;
        this.timeControl = timeControl;
        this.increment = increment;
    }

    public PichessActions getAction() {
        return action;
    }

    public int getIncrement() {
        return increment;
    }

    public int getPlayerColor() {
        return playerColor;
    }
    public String getPlayerName() {
        return playerName;
    }

    public long getTimeControl() {
        return timeControl;
    }
}
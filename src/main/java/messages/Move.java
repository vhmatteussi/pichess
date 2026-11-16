package messages;

import java.io.Serializable;
import main.Type;

public class Move implements Serializable {
    private static final long serialVersionUID = 1L;

    private int fromCol;
    private int fromRow;
    private int toCol;
    private int toRow;
    private Type promotionType;
    private long whiteTime;
    private long blackTime;

    public Move() {
    }

    public Move(int fromCol, int fromRow, int toCol, int toRow, Type promotionType, long whiteTime, long blackTime) {
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.promotionType = promotionType;
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getToCol() {
        return toCol;
    }

    public int getToRow() {
        return toRow;
    }

    public Type getPromotionType() {
        return promotionType;
    }

    public long getWhiteTime() {
        return whiteTime;
    }

    public long getBlackTime() {
        return blackTime;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(!(obj instanceof Move other)) {
            return false;
        }
        return fromCol == other.fromCol && fromRow == other.fromRow && toCol == other.toCol && toRow == other.toRow && promotionType == other.promotionType;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fromCol, fromRow, toCol, toRow, promotionType);
    }
}
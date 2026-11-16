package messages;

import java.util.ArrayList;
import pitiupi.models.Messages.Message;

public class PichessMoveMessage extends Message {
    private static final long serialVersionUID = 1L;

    private Move lastMove;
    private ArrayList<Move> moveList = new ArrayList<>();

    public PichessMoveMessage() {
        
    }

    public PichessMoveMessage(Move lastMove, ArrayList<Move> moveList) {
        this.lastMove = lastMove;
        this.moveList = moveList;
    }

    public Move getLastMove() {
        return lastMove;
    }

    public ArrayList<Move> getMoveList() {
        return moveList;
    }
}
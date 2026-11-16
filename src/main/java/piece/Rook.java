package piece;

import main.GamePanel;
import main.Type;

public class Rook extends Piece{

	public Rook(int color, int col, int row) {
		super(color, col, row);
		type = Type.ROOK;
		if(color == GamePanel.WHITE) {
			image = getImage("/piece/wR.svg");
		}
		else {
			image = getImage("/piece/bR.svg");
		}
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		if(isWithinTheBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
			if(targetCol == preCol || targetRow == preRow) {
				if(isValidMove(targetCol, targetRow) && !pieceOnStraightLine(targetCol, targetRow)) {
					return true;
				}
			}
		}
		return false;
	}
}

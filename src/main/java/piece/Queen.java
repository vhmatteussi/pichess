package piece;

import main.GamePanel;
import main.Type;

public class Queen extends Piece{

	public Queen(int color, int col, int row) {
		super(color, col, row);
		type = Type.QUEEN;
		if(color == GamePanel.WHITE) {
			image = getImage("/piece/wQ.svg");
		}
		else {
			image = getImage("/piece/bQ.svg");
		}
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		if(isWithinTheBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
			if(Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
				if(isValidMove(targetCol, targetRow) && !pieceOnDiagonalLine(targetCol, targetRow)) {
					return true;
				}
			}
			else if(targetCol == preCol || targetRow == preRow) {
				if(isValidMove(targetCol, targetRow) && !pieceOnStraightLine(targetCol, targetRow)) {
					return true;
				}
			}
		}
		return false;
	}
}

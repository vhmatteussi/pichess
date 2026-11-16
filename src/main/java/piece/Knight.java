package piece;

import main.GamePanel;
import main.Type;

public class Knight extends Piece{

	public Knight(int color, int col, int row) {
		super(color, col, row);
		type = Type.KNIGHT;
		if(color == GamePanel.WHITE) {
			image = getImage("/piece/wN.svg");
		}
		else {
			image = getImage("/piece/bN.svg");
		}
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		if(isWithinTheBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
			int horizontalMove = Math.abs(targetCol - preCol);
			int verticalMove = Math.abs(targetRow - preRow);
			if(horizontalMove * verticalMove == 2) {
				if(isValidMove(targetCol, targetRow)) {
					return true;
				}
			}
		}
		return false;
	}
}

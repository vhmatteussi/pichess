package piece;

import main.GamePanel;
import main.Type;

public class Pawn extends Piece{

	public Pawn(int color, int col, int row) {
		super(color, col, row);
		type = Type.PAWN;
		if(color == GamePanel.WHITE) {
			image = getImage("/piece/wP.svg");
		}
		else {
			image = getImage("/piece/bP.svg");
		}
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		if(isWithinTheBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
			int moveDirection;
			if(color == GamePanel.WHITE) {
				moveDirection = -1;
			}
			else {
				moveDirection = 1;
			}
			
			hittingPiece = getHittingPiece(targetCol, targetRow);
			boolean canMoveForward = targetCol == preCol && hittingPiece == null && !pieceOnStraightLine(targetCol, targetRow);
			if(canMoveForward && targetRow == preRow + moveDirection) {
				return true;
			}
			if(canMoveForward && targetRow == preRow + moveDirection * 2 && !hasMoved) {
				return true;
			}
			
			boolean diagonalMove = Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveDirection;
			if(diagonalMove && hittingPiece != null && hittingPiece.color != color) {
				return true;
			}
			
			// en passant
			if(diagonalMove) {
				for(Piece p : GamePanel.simPieces) {
					if(targetCol == p.col && preRow == p.row && p.twoStepped) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		return false;
	}
}

package piece;

import main.GamePanel;
import main.Type;

public class King extends Piece{

	public King(int color, int col, int row) {
		super(color, col, row);
		type = Type.KING;
		if(color == GamePanel.WHITE) {
			image = getImage("/piece/wK.svg");
		}
		else {
			image = getImage("/piece/bK.svg");
		}
	}
	
	private boolean isSquareAttacked(int targetCol, int targetRow) {
	    int originalCol = this.col;
	    int originalRow = this.row;
	    this.col = targetCol;
	    this.row = targetRow;
	    
	    boolean attacked = false;
	    for(Piece p: GamePanel.simPieces) {
	        if(p != this && p.color != this.color && p.type != Type.KING) {
	            if(p.canMove(targetCol, targetRow)) {
	                attacked = true;
	                break;
	            }
	        }
	    }
	    
	    this.col = originalCol;
	    this.row = originalRow;
	    return attacked;
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		if(isWithinTheBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
			int horizontalMove = Math.abs(targetCol - preCol); 
			int verticalMove = Math.abs(targetRow - preRow);
			
			if(horizontalMove + verticalMove == 1 || horizontalMove * verticalMove == 1) {
				if(isValidMove(targetCol, targetRow)) {
					return true;
				}
			}
			
			// Castling
			if(!hasMoved) {
				boolean notObstructed = pieceOnStraightLine(targetCol, targetRow) == false;
				// short castle
				if(notObstructed && targetCol == preCol + 2 && targetRow == preRow) {
					if(!isSquareAttacked(preCol + 1, preRow)) {
						for(Piece p : GamePanel.simPieces) {
							if(p.col == preCol+3 && p.row == preRow && !p.hasMoved) {
								GamePanel.castleRook = p;
								return true;
							}
						}
					}
				}
				
				// long castle
				if(notObstructed && targetCol == preCol - 2 && targetRow == preRow) {
					if(!isSquareAttacked(preCol - 1, preRow)) {
						Piece piecesOnTheWay[] = new Piece[2];
						for(Piece p : GamePanel.simPieces) {
							if(p.col == preCol-3 && p.row == preRow) {
								piecesOnTheWay[0] = p;
							}
							if(p.col == preCol-4 && p.row == preRow) {
								piecesOnTheWay[1] = p;
							}
						}
						if(piecesOnTheWay[0] == null && piecesOnTheWay[1] != null) {
							if(!piecesOnTheWay[1].hasMoved && piecesOnTheWay[1].color == this.color && piecesOnTheWay[1].type == Type.ROOK) {
								GamePanel.castleRook = piecesOnTheWay[1];
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}

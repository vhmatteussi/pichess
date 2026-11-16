package main;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import piece.Piece;

public class MoveRecord {
	public String notation;
    public Type pieceType;
    public int pieceColor;
    public boolean isCastle;
    public ArrayList<PieceSnapshot> boardState = new ArrayList<>();
    
	public static class PieceSnapshot {
		public int col, row;
		public BufferedImage image;
		
		public PieceSnapshot(Piece p) {
			this.image = p.image;
			this.col = p.col;
			this.row = p.row;
		}
	}
	
	public MoveRecord(String notation, Piece movedPiece, boolean isCastle, ArrayList<Piece> currentPieces) {
        this.notation = notation;
        this.isCastle = isCastle;
        if(movedPiece != null) {
            this.pieceType = movedPiece.type;
            this.pieceColor = movedPiece.color;
        }
        for(Piece p: currentPieces) {
            boardState.add(new PieceSnapshot(p));
        }
    }
}
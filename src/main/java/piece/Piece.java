package piece;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import main.Board;
import main.GamePanel;
import main.Type;

public class Piece {
	public Type type;
	public int x, y;
	public int col, row, preCol, preRow;
	public int color;
	private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();
	public BufferedImage image;
	public Piece hittingPiece;
	boolean hasMoved;
	public boolean twoStepped;
	
	public Piece(int color, int col, int row){
		this.color = color;
		this.col = col;
		this.row = row;
		x = getX(col);
		y = getY(row);
		preCol = col;
		preRow = row;
	}
	
	public int getX(int col) {
		return (int)col * Board.SQUARE_SIZE;
	}
	
	public int getY(int row) {
		return (int)row * Board.SQUARE_SIZE;
	}
	
	public int getCol(int x) {
		return (x + Board.HALF_SQUARE_SIZE)/Board.SQUARE_SIZE;
	}
	
	public int getRow(int y) {
		return (y + Board.HALF_SQUARE_SIZE)/Board.SQUARE_SIZE;
	}
	
	public int getIndex() {
		for(int i=0; i<GamePanel.simPieces.size(); i++) {
			if(GamePanel.simPieces.get(i) == this) {
				return i;
			}
		}
		return -1;
	}
	
	public void updatePosition() {
		if(type == Type.PAWN) {
			if(Math.abs(row - preRow) == 2) {
				twoStepped = true;
			}
		}
		
		x = getX(col);
		y = getY(row);
		preCol = getCol(x);
		preRow = getRow(y);
		hasMoved = true;
	}
	
	public void resetPosition() {
		col = preCol;
		row = preRow;
		x = getX(col);
		y = getY(row);
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		return false;
	}
	
	public boolean isWithinTheBoard(int targetCol, int targetRow) {
		if(targetCol >=0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7) {
			return true;
		}
		return false;
	}
	
	public boolean isSameSquare(int targetCol, int targetRow) {
		if(targetCol == preCol && targetRow == preRow) {
			return true;
		}
		return false;
	}
	
	public Piece getHittingPiece(int targetCol, int targetRow) {
		for(Piece p : GamePanel.simPieces) {
			if(p != null) {
				if(p.col == targetCol && p.row == targetRow && p != this) {
					return p;
				}
			}
		}
		return null;
	}
	
	public boolean isValidMove(int targetCol, int targetRow) {
		hittingPiece = getHittingPiece(targetCol, targetRow);
		
		if(hittingPiece == null) {
			return true;
		}
		else {
			if(hittingPiece.color != this.color) {
				return true;
			}
			else {
				hittingPiece = null;
			}
		}
		return false;
	}
	
	public boolean pieceOnStraightLine(int targetCol, int targetRow) {
		for(int c=preCol-1; c > targetCol; c--) {
			for(Piece p: GamePanel.simPieces) {
				if(p != null) {
					if(p.col == c && p.row == targetRow) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		for(int c=preCol+1; c < targetCol; c++) {
			for(Piece p: GamePanel.simPieces) {
				if(p != null) {
					if(p.col == c && p.row == targetRow) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		for(int r=preRow+1; r < targetRow; r++) {
			for(Piece p: GamePanel.simPieces) {
				if(p != null) {
					if(p.col == targetCol && p.row == r) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		for(int r=preRow-1; r > targetRow; r--) {
			for(Piece p: GamePanel.simPieces) {
				if(p != null) {
					if(p.col == targetCol && p.row == r) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean pieceOnDiagonalLine(int targetCol, int targetRow) {
		if(targetRow < preRow) {
			for(int c=preCol-1; c > targetCol; c--) {
				int diff = Math.abs(c - preCol);
				for(Piece p: GamePanel.simPieces) {
					if(p.col == c && p.row == preRow - diff) {
						hittingPiece = p;
						return true;
					}
				}
			}
			
			for(int c=preCol+1; c < targetCol; c++) {
				int diff = Math.abs(c - preCol);
				for(Piece p: GamePanel.simPieces) {
					if(p.col == c && p.row == preRow - diff) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		if(targetRow > preRow) {
			for(int c=preCol+1; c < targetCol; c++) {
				int diff = Math.abs(c - preCol);
				for(Piece p: GamePanel.simPieces) {
					if(p.col == c && p.row == preRow + diff) {
						hittingPiece = p;
						return true;
					}
				}
			}
			
			for(int c=preCol-1; c > targetCol; c--) {
				int diff = Math.abs(c - preCol);
				for(Piece p: GamePanel.simPieces) {
					if(p.col == c && p.row == preRow + diff) {
						hittingPiece = p;
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public BufferedImage getImage(String imagePath) {
		if(IMAGE_CACHE.containsKey(imagePath)) {
			return IMAGE_CACHE.get(imagePath);
		}

		FlatSVGIcon svgIcon = new FlatSVGIcon(getClass().getResource(imagePath)).derive(Board.SQUARE_SIZE, Board.SQUARE_SIZE);
		BufferedImage bufferedImage = new BufferedImage(Board.SQUARE_SIZE, Board.SQUARE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bufferedImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		svgIcon.paintIcon(null, g2, 0, 0);
		g2.dispose();

		IMAGE_CACHE.put(imagePath, bufferedImage);
		return bufferedImage;
	}
	
	public void draw(Graphics2D g) {
		g.drawImage(image, x, y, null);
	}
	
	public void drawPre(Graphics2D g) {
		int preX = getX(preCol);
		int preY = getY(preRow);
		g.drawImage(image, preX, preY, null);
	}
	
	public void drawSmallSize(Graphics2D g, int size) {
		int offset = (Board.SQUARE_SIZE - size) / 2;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, x + offset, y + offset, size, size, null);
	}
}

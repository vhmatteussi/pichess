package main;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import main.CustomOptionPane.HostPlayerColor;
import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.function.BiConsumer;
import messages.PichessActionMessage;
import messages.PichessMoveMessage;
import pitiupi.application.presence.UserPeerView;
import pitiupi.models.Messages.Message;

import messages.Move;

public class GamePanel extends JPanel implements Runnable {
	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	
	final int FPS = 144;
	Thread gameThread;
	
	Board board = new Board();
	Mouse mouse = new Mouse();
	
	// game turns
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	int currentColor = WHITE;
	
	// chess clock variables
	public long timeControl = 10 * 60 * 1000;
	public int increment = 0;
	
	long whiteTime = timeControl;
	long blackTime = timeControl;
	
	// arrays
	public static ArrayList<Piece> pieces = new ArrayList<>();
	public static ArrayList<Piece> simPieces = new ArrayList<>();
	ArrayList<Piece> promotionPieces = new ArrayList<>();
	ArrayList<int[]> validMoves = new ArrayList<>();
	
	// move history
	ArrayList<MoveRecord> moveHistory = new ArrayList<>();
	int historyIndex = -1;
	
	// auxiliary "pieces"
	Piece activePiece, checkingPiece, checkedKing;
	public static Piece castleRook;
	
	// network control variables
	InetAddress opponentAddress;
	String opponentName;
	String localPlayerName = "player1"; // useless logic, but too late to change
	BiConsumer<InetAddress, Message> messageSender;
	int localColor = WHITE;
	int opponentColor = BLACK;
	boolean matchAccepted = true;
	String firstPlayerName = "player1";
	String secondPlayerName = "player2";
	boolean whiteConnected = true;
	boolean blackConnected = true;
	long lastClockUpdate = System.currentTimeMillis();
	HostPlayerColor hostColor;
	PichessActionMessage.PichessActions action = null;
	
	Move moveReceived;
	ArrayList<Move> moveListReceived;
	ArrayList<Move> localMoveList = new ArrayList<>();
	final Network network = new Network(this);
	final SidebarRenderer sidebarRenderer;
	private final JPanel boardPanel;
	private String lastSidebarState = "";
	
	// auxiliary booleans
	boolean canMove;
	boolean validMove;
	boolean promotion;
	
	// possible results
	boolean gameover;
	boolean checkmate;
	boolean stalemate;
	boolean insufficientMaterial;
	boolean drawByRepetition;
	
	// network dependent results
	boolean timeout;
	boolean calledDraw;
	boolean calledResign;
	boolean confirmDraw;
	boolean confirmResign;
	boolean gameAborted;
	boolean resultPopupShown;
	boolean rematchRequestedLocally;
	boolean rematchRequestedByOpponent;
	boolean rematchStarted;
	private JDialog gameoverDialog;
	private JButton rematchButton;
	
	public GamePanel() {
		super(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setLayout(new BorderLayout());
		this.setFocusable(true);
		
		pieces.clear();
		simPieces.clear();
		castleRook = null;
		setPieces();
		copyPieces(pieces, simPieces);
		
		moveHistory.add(new MoveRecord("Start", null, false, pieces));
		historyIndex = 0;

		boardPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawBoardScene((Graphics2D) g);
			}
		};
		boardPanel.setPreferredSize(new Dimension(Board.SQUARE_SIZE * 8, Board.SQUARE_SIZE * 8));
		boardPanel.addMouseMotionListener(mouse);
		boardPanel.addMouseListener(mouse);
		add(boardPanel, BorderLayout.CENTER);

		sidebarRenderer = new SidebarRenderer(this);
		add(sidebarRenderer, BorderLayout.EAST);
		
		this.addKeyListener(new java.awt.event.KeyAdapter() {
		    @Override
		    public void keyPressed(java.awt.event.KeyEvent e) {
		        if(e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
		            if(historyIndex > 0) {
		                historyIndex--;
		                activePiece = null;
		            }
		        }
		        else if(e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) {
		            if(historyIndex < moveHistory.size() - 1) {
		                historyIndex++;
		                activePiece = null;
		            }
		        }
		    }
		});
	}

	public void setLocalPlayerName(String localPlayerName) {
		network.setLocalPlayerName(localPlayerName);
	}

	public InetAddress getOpponentAddress() {
		return opponentAddress;
	}

	public String getOpponentName() {
		return opponentName;
	}

	public void updateConnection(boolean localOnline, boolean opponentOnline) {
		boolean whiteConnectedNow;
		boolean blackConnectedNow;

		if(opponentAddress == null) {
			whiteConnectedNow = (localColor == WHITE) && localOnline;
			blackConnectedNow = (localColor == BLACK) && localOnline;
		}
		else if(localColor == WHITE) {
			whiteConnectedNow = localOnline;
			blackConnectedNow = opponentOnline;
		}
		else {
			whiteConnectedNow = opponentOnline;
			blackConnectedNow = localOnline;
		}

		boolean changed = (whiteConnected != whiteConnectedNow) || (blackConnected != blackConnectedNow);
		whiteConnected = whiteConnectedNow;
		blackConnected = blackConnectedNow;

		// if opponent leaves, can't offer rematch
		if(!opponentOnline && gameover && (gameoverDialog != null || rematchRequestedLocally || rematchRequestedByOpponent)) {
			rematchRequestedLocally = false;
			rematchRequestedByOpponent = false;
			rematchButton.setOpaque(true);
		}

		if(changed) {
			sidebarRenderer.refresh();
		}
	}

	void resetGameState() {
		pieces.clear();
		simPieces.clear();
		promotionPieces.clear();
		validMoves.clear();
		castleRook = null;
		activePiece = null;
		checkingPiece = null;
		checkedKing = null;

		currentColor = WHITE;
		whiteTime = timeControl;
		blackTime = timeControl;
		lastClockUpdate = System.currentTimeMillis();

		moveHistory.clear();
		moveHistory.add(new MoveRecord("Start", null, false, pieces));
		setPieces();
		copyPieces(pieces, simPieces);
		moveHistory.set(0, new MoveRecord("Start", null, false, pieces));
		historyIndex = 0;

		localMoveList.clear();
		moveReceived = null;
		moveListReceived = null;

		canMove = false;
		validMove = false;
		promotion = false;
		gameover = false;
		checkmate = false;
		stalemate = false;
		insufficientMaterial = false;
		drawByRepetition = false;
		timeout = false;
		calledDraw = false;
		calledResign = false;
		confirmDraw = false;
		confirmResign = false;
		gameAborted = false;
		resultPopupShown = false;
		rematchRequestedLocally = false;
		rematchRequestedByOpponent = false;
		rematchStarted = false;
		closeGameoverPopup();
		lastSidebarState = "";
		SwingUtilities.invokeLater(sidebarRenderer::refresh);
	}

	void startRematch() {
		if(rematchStarted) {
			return;
		}
		rematchStarted = true;
		closeGameoverPopup();

		// swap colors on rematch
		if(localColor == WHITE){
			localColor = BLACK;
		}
		else{
			localColor = WHITE;
		}

		if(localColor == WHITE){
			opponentColor = BLACK;
		}
		else{
			opponentColor = WHITE;
		}

		String aux = firstPlayerName;
		firstPlayerName = secondPlayerName;
		secondPlayerName = aux;

		resetGameState();
		matchAccepted = true;
	}

	boolean isBoardFlipped() {
		return localColor == BLACK;
	}

	private int toScreenCol(int boardCol) {
		if(isBoardFlipped()) {
			return (7 - boardCol);
		}
		return boardCol;
	}

	private int toScreenRow(int boardRow) {
		if(isBoardFlipped()) {
			return (7 - boardRow);
		}
		return boardRow;
	}

	private int toBoardCol(int screenX) {
		int screenCol = Math.max(0, Math.min(7, screenX / Board.SQUARE_SIZE));
		if(isBoardFlipped()) {
			return (7 - screenCol);
		}
		return screenCol;
	}

	private int toBoardRow(int screenY) {
		int screenRow = Math.max(0, Math.min(7, screenY / Board.SQUARE_SIZE));
		if(isBoardFlipped()) {
			return (7 - screenRow);
		}
		return screenRow;
	}

	public void startNetworkGame(UserPeerView peer, int minutes, int increment, HostPlayerColor hostColor) {
		network.startNetworkGame(peer, minutes, increment, hostColor);
	}
	
	public void messageSender(BiConsumer<InetAddress, Message> messageSender) {
		network.messageSender(messageSender);
	}

	protected void sendMessage(Message message) {
		network.sendMessage(message);
	}

	public void getMoveMessage(PichessMoveMessage moveMessage) {
		network.getMoveMessage(moveMessage);
	}

	public void getActionMessage(PichessActionMessage actionMessage) {
		network.getActionMessage(actionMessage, null);
	}

	public void getActionMessage(PichessActionMessage actionMessage, InetAddress senderAddress){
		network.getActionMessage(actionMessage, senderAddress);
	}

	public void launchGame() {
		if(gameThread != null && gameThread.isAlive()) {
			return;
		}
		gameThread = new Thread(this, "pichess-game-thread");
		lastClockUpdate = System.currentTimeMillis();
		gameThread.start();
	}

	public void stopGame() {
		Thread thread = gameThread;
		gameThread = null;
		if(thread != null) {
			thread.interrupt();
		}
	}
	
	public void setPieces() {
		// white
		pieces.add(new Pawn(WHITE, 0, 6));
		pieces.add(new Pawn(WHITE, 1, 6));
		pieces.add(new Pawn(WHITE, 2, 6));
		pieces.add(new Pawn(WHITE, 3, 6));
		pieces.add(new Pawn(WHITE, 4, 6));
		pieces.add(new Pawn(WHITE, 5, 6));
		pieces.add(new Pawn(WHITE, 6, 6));
		pieces.add(new Pawn(WHITE, 7, 6));
		pieces.add(new Rook(WHITE, 0, 7));
		pieces.add(new Rook(WHITE, 7, 7));
		pieces.add(new Knight(WHITE, 1, 7));
		pieces.add(new Knight(WHITE, 6, 7));
		pieces.add(new Bishop(WHITE, 2, 7));
		pieces.add(new Bishop(WHITE, 5, 7));
		pieces.add(new Queen(WHITE, 3, 7));
		pieces.add(new King(WHITE, 4, 7));
		
		// black
		pieces.add(new Pawn(BLACK, 0, 1));
		pieces.add(new Pawn(BLACK, 1, 1));
		pieces.add(new Pawn(BLACK, 2, 1));
		pieces.add(new Pawn(BLACK, 3, 1));
		pieces.add(new Pawn(BLACK, 4, 1));
		pieces.add(new Pawn(BLACK, 5, 1));
		pieces.add(new Pawn(BLACK, 6, 1));
		pieces.add(new Pawn(BLACK, 7, 1));
		pieces.add(new Rook(BLACK, 0, 0));
		pieces.add(new Rook(BLACK, 7, 0));
		pieces.add(new Knight(BLACK, 1, 0));
		pieces.add(new Knight(BLACK, 6, 0));
		pieces.add(new Bishop(BLACK, 2, 0));
		pieces.add(new Bishop(BLACK, 5, 0));
		pieces.add(new Queen(BLACK, 3, 0));
		pieces.add(new King(BLACK, 4, 0));
	}
	
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
		target.clear();
		for(int i=0; i<source.size(); i++) {
				target.add(source.get(i));	
		}
	}
	
	@Override
	public void run() {
		double drawInterval = 1_000_000_000.0 / FPS;
		double delta = 0;
	    long lastTime = System.nanoTime();
	    long currentTime;
		
		while(gameThread != null) {
			currentTime = System.nanoTime();
	        delta += (currentTime - lastTime) / drawInterval;
	        lastTime = currentTime;
	        if(delta >= 1) {
	            update();
	            boardPanel.repaint();
	            delta--;
	        }
	        else{
	            try {
	                Thread.sleep(1);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	            }
	        }
		}
	}
	
	private void update() {
		// clock
		long now = System.currentTimeMillis();
		long elapsed = now - lastClockUpdate;
		lastClockUpdate = now;

		if(!gameover && moveHistory.size() > 1) {
		    if(currentColor == WHITE) {
		        whiteTime -= elapsed;
		        if(whiteTime <= 0) {
		            whiteTime = 0;
		            timeout = true;
		            gameover = true;
		            activePiece = null;
		        }
		    } 
		    else {
		        blackTime -= elapsed;
		        if(blackTime <= 0) {
		            blackTime = 0;
		            timeout = true;
		            gameover = true;
		            activePiece = null;
		        }
		    }
		}

		// open result popup
		if(gameover && !resultPopupShown) {
			resultPopupShown = true;
			SwingUtilities.invokeLater(this::showGameoverPopup);
		}
		refreshSidebar();
		
		// movement history
		boolean isViewingHistory = (historyIndex < moveHistory.size() - 1);
		if(isViewingHistory) {
			// if clicked while viewing history, return to current position
			if(mouse.pressed && mouse.x < 800) {
				historyIndex = moveHistory.size() - 1;
				mouse.pressed = false;
			}
			return;
		}

		if(!matchAccepted) {
			return;
		}

		// local piece handling
		if(currentColor == localColor){
			// pawn promotion handling
			if(promotion == true) {
				promoting();
			}
			else if(gameover == false) {
				// holding a piece
				if(mouse.pressed) {
					if(activePiece == null) {
						int targetCol = toBoardCol(mouse.x);
						int targetRow = toBoardRow(mouse.y);
						for(Piece p: simPieces) {
							if(p.color == currentColor && p.col == targetCol && p.row == targetRow) {
								activePiece = p;
								getValidMoves(activePiece);
								break;
							}
						}
					}
					else {
						simulate();
					}
				}
				// releasing a piece
				if(mouse.pressed == false){
					if(activePiece != null) {
						if(validMove) {
							int oldCol = activePiece.preCol;
							int oldRow = activePiece.preRow;
							boolean isCapture = (activePiece.hittingPiece != null);
							boolean isCastle = (castleRook != null);
							Piece movedPiece = activePiece;
							
							copyPieces(simPieces, pieces);
							activePiece.updatePosition();
							
							if(castleRook != null) {
								checkCastling();
								castleRook.updatePosition();
							}
							
							if(canPromote()) {
								promotion = true;
							}
							else {
								finalizeMove(movedPiece, oldCol, oldRow, isCapture, isCastle, null);
							}
						}
						else {
							copyPieces(pieces, simPieces);
							activePiece.resetPosition();
							activePiece = null;
						}
					}
				}
			}
		}
		// network piece handling
		else {
			if(moveReceived != null) {
				if(promotion == true) {
					promoting();
				}
				else if(!gameover) {
					for(Piece p: simPieces) {
						if(p.color == currentColor && p.col == moveReceived.getFromCol() && p.row == moveReceived.getFromRow()) {
							activePiece = p;
							getValidMoves(activePiece);
							break;
						}
					}
					if(activePiece != null) {
						activePiece.col = moveReceived.getToCol();
						activePiece.row = moveReceived.getToRow();
						activePiece.x = activePiece.getX(activePiece.col);
						activePiece.y = activePiece.getY(activePiece.row);
						simulateAt(moveReceived.getToCol(), moveReceived.getToRow());

						if(validMove) {
							localMoveList.add(moveReceived);
							int oldCol = activePiece.preCol;
							int oldRow = activePiece.preRow;
							boolean isCapture = (activePiece.hittingPiece != null);
							boolean isCastle = (castleRook != null);
							Piece movedPiece = activePiece;
							
							copyPieces(simPieces, pieces);
							activePiece.updatePosition();
							
							if(castleRook != null) {
								checkCastling();
								castleRook.updatePosition();
							}
							
							if(canPromote()) {
								promotion = true;
							}
							else {
								finalizeMove(movedPiece, oldCol, oldRow, isCapture, isCastle, null);
							}
						}
						else {
							copyPieces(pieces, simPieces);
							activePiece.resetPosition();
							activePiece = null;
							moveReceived = null;
						}
					}
				}
			}
		}
		
	}
	
	// local piece simulation
	private void simulate() {
		activePiece.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activePiece.y = mouse.y - Board.HALF_SQUARE_SIZE;
		simulateAt(toBoardCol(mouse.x), toBoardRow(mouse.y));
	}

	// generalized piece simulation
	private void simulateAt(int targetCol, int targetRow) {
		canMove = false;
		validMove = false;
		activePiece.hittingPiece = null;
		
		copyPieces(pieces, simPieces);
		castleRook = null;
		
		activePiece.col = targetCol;
		activePiece.row = targetRow;
		
		if(activePiece.canMove(activePiece.col, activePiece.row)) {
			canMove = true;
			if(activePiece.hittingPiece != null) {
				int index = activePiece.hittingPiece.getIndex();
				if(index != -1) {
					simPieces.remove(index);
				}
			}
			
			if(!isIllegal(activePiece) && !canCaptureKing()) {
				validMove = true;
			}
			else{
				validMove = false;
			}
		}
	}
	
	public boolean isIllegal(Piece king) {
		if(king.type == Type.KING) {
			for(int i = 0; i < simPieces.size(); i++) {
				Piece p = simPieces.get(i);
				// if a piece can move to that square, the king can't
				if(p != king && p.color != king.color && p.canMove(king.col, king.row)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean canCaptureKing() {
		Piece king = getKing(false);
		for(int i=0; i<simPieces.size(); i++) {
			Piece p = simPieces.get(i);
			// simmilar to above, but one move before, so a piece is obligated to block a check
			if(p.color != king.color && p.canMove(king.col, king.row)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isKingInCheck() {
		Piece king = getKing(true);
	    for(int i=0; i<simPieces.size(); i++) {
	        Piece p = simPieces.get(i);
	        if(p.color == currentColor && p.canMove(king.col, king.row)) {
	        	checkingPiece = p;
	            checkedKing = king;
	            return true;
	        }
	    }
	    checkingPiece = null;
	    checkedKing = null;
	    return false;
	}
	
	private Piece getKing(boolean opponent) {
		Piece king = null;
		for(Piece p: simPieces) {
			if(p != null) {
				if(opponent) {
					if(p.type == Type.KING && p.color != currentColor) {
						king = p;
					}
				}
				else {
					if(p.type == Type.KING && p.color == currentColor) {
						king = p;
					}
				}
			}
		}
		return king;
	}
	
	private boolean isStalemate() {
	    if(!isKingInCheck()) {
	    	if(currentColor == WHITE) {
				if(!hasAnyValidMove(BLACK)) {
					return true;
				}
			}
			else {
				if(!hasAnyValidMove(WHITE)) {
					return true;
				}
	        }
	    }
	    return false;
	}
	
	private boolean isCheckmate() {
		if(isKingInCheck()) {
			if(currentColor == WHITE) {
				if(!hasAnyValidMove(BLACK)) {
					return true;
				}
			}
			else {
				if(!hasAnyValidMove(WHITE)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean hasAnyValidMove(int color) {
		int originalTurn = currentColor;
		currentColor = color;
		ArrayList<Piece> piecesCopy = new ArrayList<>(pieces);
		
		for(Piece p: piecesCopy) {
			if(p.color == color) {
				getValidMoves(p);
				if(!validMoves.isEmpty()) {
					validMoves.clear();
					currentColor = originalTurn;
					return true;
				}
			}
		}
		validMoves.clear();
		currentColor = originalTurn;
		return false;
	}
	
	public void checkCastling() {
		if(castleRook != null) {
			if(castleRook.col == 0) {
				// aRd1
				castleRook.col += 3;
			}
			else if(castleRook.col == 7) {
				// hRf1
				castleRook.col -= 2;
			}
			castleRook.x = castleRook.getX(castleRook.col);
		}
	}
	
	public void changeTurn() {
		if(currentColor == WHITE) {
			whiteTime += increment;
			currentColor = BLACK;
			// disables flag for en passant
			for(Piece p: pieces) {
				if(p.color == BLACK) {
					p.twoStepped = false;
				}
			}
		}
		else {
			blackTime += increment;
			currentColor = WHITE;
			for(Piece p: pieces) {
				if(p.color == WHITE) {
					p.twoStepped = false;
				}
			}
		}
		activePiece = null;
	}
	
	private boolean canPromote() {
		// lichess style promotion, where all promotion pieces available appear on the same col as the promoted pawn
		if(activePiece.type == Type.PAWN) {
			int promoCol = activePiece.col;
			if(currentColor == WHITE) {
				if(activePiece.row == 0) {
					promotionPieces.clear();
					promotionPieces.add(new Queen(currentColor, promoCol, 0));
					promotionPieces.add(new Knight(currentColor, promoCol, 1));
					promotionPieces.add(new Rook(currentColor, promoCol, 2));
					promotionPieces.add(new Bishop(currentColor, promoCol, 3));
					return true;
				}
			}
			else {
				if(activePiece.row == 7) {
					promotionPieces.clear();
					promotionPieces.add(new Queen(currentColor, promoCol, 7));
					promotionPieces.add(new Knight(currentColor, promoCol, 6));
					promotionPieces.add(new Rook(currentColor, promoCol, 5));
					promotionPieces.add(new Bishop(currentColor, promoCol, 4));
					return true;
				}
			}
		}
		return false;
	}
	
	private void promoting() {
		int oldCol = activePiece.preCol;
	    int oldRow = activePiece.preRow;
	    boolean isCapture = (activePiece.hittingPiece != null);
	    boolean isCastle = (castleRook != null);
	    Piece movedPiece = activePiece;
		if(moveReceived == null){
			if(mouse.pressed) {
				int targetCol = toBoardCol(mouse.x);
				int targetRow = toBoardRow(mouse.y);
				for(Piece p: promotionPieces) {
					if(p.col == targetCol && p.row == targetRow) {
						switch(p.type) {
							case QUEEN: 
								simPieces.add(new Queen(currentColor, activePiece.col, activePiece.row));
								break;
							case KNIGHT: 
								simPieces.add(new Knight(currentColor, activePiece.col, activePiece.row));
								break;
							case ROOK: 
								simPieces.add(new Rook(currentColor, activePiece.col, activePiece.row));
								break;
							case BISHOP: 
								simPieces.add(new Bishop(currentColor, activePiece.col, activePiece.row));
								break;
							default:
								break;
						}
						simPieces.remove(activePiece.getIndex());
						copyPieces(simPieces, pieces);
						activePiece = null;
						promotion = false;
						finalizeMove(movedPiece, oldCol, oldRow, isCapture, isCastle, p.type);
						break;
					}
				}
			}
		}
		else{
			switch(moveReceived.getPromotionType()) {
				case QUEEN: 
					simPieces.add(new Queen(currentColor, activePiece.col, activePiece.row));
					break;
				case KNIGHT: 
					simPieces.add(new Knight(currentColor, activePiece.col, activePiece.row));
					break;
				case ROOK: 
					simPieces.add(new Rook(currentColor, activePiece.col, activePiece.row));
					break;
				case BISHOP: 
					simPieces.add(new Bishop(currentColor, activePiece.col, activePiece.row));
					break;
				default:
					break;
			}
			simPieces.remove(activePiece.getIndex());
			copyPieces(simPieces, pieces);
			activePiece = null;
			promotion = false;
			finalizeMove(movedPiece, oldCol, oldRow, isCapture, isCastle, moveReceived.getPromotionType());
		}
	}
	
	private void finalizeMove(Piece movedPiece, int oldCol, int oldRow, boolean isCapture, boolean isCastle, Type promoType) {
		// origianlly on Riy's update, but its more clean in another method
	    if(isCheckmate()) {
	        gameover = true;
	        checkmate = true;
	    } 
	    else if(isStalemate()) {
	        stalemate = true;
	        gameover = true;
	    }
	    else if(isInsufficientMaterial()) {
	        insufficientMaterial = true;
	        gameover = true;
	    }
	    
	    String notation = getMoveNotation(movedPiece, oldCol, oldRow, isCapture, isCastle, promoType);
	    moveHistory.add(new MoveRecord(notation, movedPiece, isCastle, pieces));
	    historyIndex = moveHistory.size() - 1;
	    
	    if(isThreefoldRepetition()) {
	        drawByRepetition = true;
	        gameover = true;
	    }

	    if(gameover) {
	        activePiece = null;
	    }
	    else {
	        changeTurn();
	    }

	    boolean wasRemoteMove = (moveReceived != null);
	    moveReceived = null;
	    if (!wasRemoteMove && opponentAddress != null) {
	        Move sentMove = new Move(oldCol, oldRow, movedPiece.col, movedPiece.row, promoType, whiteTime, blackTime);
	        localMoveList.add(sentMove);
	        sendMessage(new PichessMoveMessage(sentMove, new ArrayList<>(localMoveList)));
	    }
	}
	
	private boolean isInsufficientMaterial() {
	    int numKnights = 0;
	    int knightColor = -1;
	    boolean sameColorKnights = true;

	    int numBishops = 0;
	    boolean hasLightSquareBishop = false;
	    boolean hasDarkSquareBishop = false;

	    for(Piece p: pieces) {
	        if(p.type == Type.KING) {
	            continue;
	        }
	        if(p.type == Type.BISHOP) {
	            numBishops++;
	            if((p.row + p.col) % 2 == 0) {
	                hasLightSquareBishop = true;
	            }
	            else {
	                hasDarkSquareBishop = true;
	            }
	        }
	        else if(p.type == Type.KNIGHT) {
	            if(knightColor == -1) {
	                knightColor = p.color;
	            }
	            else if(p.color != knightColor) {
	                sameColorKnights = false;
	            }
	            numKnights++;
	        }
	        else {
	            return false;
	        }
	    }

	    if(numBishops == 0 && numKnights == 0) {
	        return true;
	    }
	    
	    if(numKnights == 0 && !(hasLightSquareBishop && hasDarkSquareBishop)) {
	        return true;
	    }

	    if(numBishops == 0) {
	        if(numKnights == 1) {
	            return true;
	        }
	        if(numKnights == 2 && sameColorKnights) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private boolean isThreefoldRepetition() {
		if(moveHistory.size() < 9) {
			return false;
		}
	    MoveRecord latest = moveHistory.get(moveHistory.size() - 1);
	    int matches = 0;
	    
	    for(int i=moveHistory.size()-1; i>=0; i-=2) {
	        MoveRecord past = moveHistory.get(i);
	        if(past.boardState.size() != latest.boardState.size()) {
	        	break;
	        }
	        
	        int samePieces = 0;
	        for(MoveRecord.PieceSnapshot s1: latest.boardState) {
	            for(MoveRecord.PieceSnapshot s2: past.boardState) {
	                if(s1.col == s2.col && s1.row == s2.row && s1.image == s2.image) {
	                    samePieces++;
	                    break;
	                }
	            }
	        }
	        if(samePieces == latest.boardState.size()) {
	            matches++;
	            if(matches >= 3) {
	            	return true;
	            }
	        }
	    }
	    return false;
	}
	
	private void getValidMoves(Piece p) {
		validMoves.clear();
		int originalCol = p.col;
		int originalRow = p.row;
		Piece originalCastleRook = castleRook;
		
		for(int c=0; c<8; c++) {
			for(int r=0; r<8; r++) {
				copyPieces(pieces, simPieces);
				p.hittingPiece = null;
				castleRook = null;
				p.col = originalCol;
				p.row = originalRow;
				
				if(p.canMove(c, r)) {
					p.col = c;
					p.row = r;
					
					if(p.hittingPiece != null) {
						int index = p.hittingPiece.getIndex();
						if(index != -1) {
							simPieces.remove(index);
						}
					}
					if(!isIllegal(p) && !canCaptureKing()) {
						validMoves.add(new int[] {c, r});
					}
				}
			}
		}
		p.col = originalCol;
	    p.row = originalRow;
	    p.hittingPiece = null;
	    castleRook = originalCastleRook;
	    copyPieces(pieces, simPieces);
	}
	
	String getPieceSymbol(MoveRecord record) {
		String symbol = "";
	    if(record.isCastle || record.pieceType == null || record.pieceType == Type.PAWN) {
	        return symbol;
	    }
	    if(record.pieceColor == WHITE) {
	        switch(record.pieceType){
	            case KNIGHT:
	            	symbol = "♞";
	            	break;
	            case BISHOP:
		            symbol = "♝";
	            	break;
	            case ROOK:
		            symbol = "♜";
	            	break;
	            case QUEEN:
		            symbol = "♛";
	            	break;
	            case KING:
		            symbol = "♚";
	            	break;
	            default:
	            	symbol = "";
	            	break;
	        }
	    } 
	    else {
	    	switch(record.pieceType){
	            case KNIGHT:
	            	symbol = "♘";
	            	break;
	            case BISHOP:
		            symbol = "♗";
	            	break;
	            case ROOK:
		            symbol = "♖";
	            	break;
	            case QUEEN:
		            symbol = "♕";
	            	break;
	            case KING:
		            symbol = "♔";
	            	break;
	            default:
	            	symbol = "";
	            	break;
	        }
	    }
	    return symbol;
	}
	
	private String getMoveNotation(Piece p, int preCol, int preRow, boolean isCapture, boolean isCastle, Type promoType) {
		if(isCastle) {
			if(p.col == 6) {
				return "O-O";
			}
			else {
				return "O-O-O";
			}
		}
		
		StringBuilder sb = new StringBuilder();
		char file = (char)('a' + p.col);
		int rank = 8 - p.row;
		
		switch(p.type) {
			case KNIGHT:
				sb.append("N");
				break;
			case BISHOP:
				sb.append("B");
				break;
			case ROOK:
				sb.append("R");
				break;
			case QUEEN:
				sb.append("Q");
				break;
			case KING:
				sb.append("K");
				break;
			default:
				break;
		}
		
		if(isCapture) {
			if(p.type == Type.PAWN) {
				sb.append((char)('a' + preCol));
			}
			sb.append("x");
		}
		sb.append(file).append(rank);
		
		if(promoType != null) {
			sb.append("=");
			switch(promoType) {
				case KNIGHT:
					sb.append("N");
					break;
				case BISHOP:
					sb.append("B");
					break;
				case ROOK:
					sb.append("R");
					break;
				case QUEEN:
					sb.append("Q");
					break;
				default:
					break;
			}
		}
		
		if(checkmate) {
			sb.append("#");
		}
		else if(checkedKing != null) {
			sb.append("+");
		}
		return sb.toString();
	}
	
	void drawButtonClicked() {
		if(gameover) {
			return;
		}
		confirmResign = false;
		if(!confirmDraw) {
			confirmDraw = true;
		}
		else {
			confirmDraw = false;
			if(opponentAddress != null) {
				sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.OFFER_DRAW, localPlayerName, localColor,timeControl, increment));
			}
			else {
				calledDraw = true;
				gameover = true;
				activePiece = null;
			}
		}
		sidebarRenderer.refresh();
	}

	void resignButtonClicked() {
		if(gameover) {
			return;
		}
		confirmDraw = false;
		if(!confirmResign) {
			confirmResign = true;
		}
		else {
			calledResign = true;
			gameover = true;
			confirmResign = false;
			activePiece = null;
			if(opponentAddress != null) {
				sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.RESIGN, localPlayerName, localColor,timeControl, increment));
			}
		}
		sidebarRenderer.refresh();
	}

	private void refreshSidebar() {
		String state = sidebarRenderer.formatTime(whiteTime) + "|" + sidebarRenderer.formatTime(blackTime) + "|" + moveHistory.size() + "|";
		state = state + currentColor + "|" + gameover + "|" + confirmDraw + "|" + confirmResign + "|";
		state = state + firstPlayerName + "|" + secondPlayerName + "|" + whiteConnected + "|" + blackConnected;
		if(!state.equals(lastSidebarState)) {
			lastSidebarState = state;
			SwingUtilities.invokeLater(sidebarRenderer::refresh);
		}
	}

	private String getGameResultMessage() {
		if(checkmate) {
			if(currentColor == WHITE) {
				return "1 - 0" + " | Xeque-mate";
			}
			else {
				return "0 - 1" + " | Xeque-mate";
			}
		}
		if(timeout) {
			if(currentColor == WHITE) {
				return "1 - 0" + " | Tempo esgotado";
			}
			else {
				return "0 - 1" + " | Tempo esgotado";
			}
		}
		if(calledResign) {
			if(currentColor == WHITE) {
				return "1 - 0" + " | Desistência";
			}
			else {
				return "0 - 1" + " | Desistência";
			}
		}
		if(stalemate) {
			return "½ - ½ | Afogamento";
		}
		if(calledDraw) {
			return "½ - ½ | Empate";
		}
		if(insufficientMaterial) {
			return "½ - ½ | Material insuficiente";
		}
		if(drawByRepetition) {
			return "½ - ½ | Empate por repetição";
		}
		if(gameAborted) {
			return "Partida encerrada por dessincronização";
		}
		return "Fim de Jogo";
	}

	void closeGameoverPopup() {
		if(gameoverDialog != null) {
			gameoverDialog.dispose();
			gameoverDialog = null;
		}
		rematchButton = null;
	}

	void updateRematchButton() {
		if(rematchButton == null) {
			return;
		}
		if(rematchRequestedLocally) {
			rematchButton.setText("Revanche");
			rematchButton.setEnabled(false);
			rematchButton.setOpaque(true);
		}
		else if(rematchRequestedByOpponent) {
			rematchButton.setText("Aceitar revanche");
			rematchButton.setEnabled(true);
		}
		else {
			rematchButton.setText("Revanche");
			rematchButton.setEnabled(true);
		}
		if(gameoverDialog != null) {
			gameoverDialog.pack();
		}
	}

	private void rematchButtonClicked() {
		if(!gameover || rematchStarted || rematchRequestedLocally) {
			return;
		}
		if(opponentAddress == null) {
			startRematch();
			return;
		}
		if(rematchRequestedByOpponent) {
			rematchRequestedLocally = true;
			sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.ACCEPT_REMATCH, localPlayerName, localColor, timeControl, increment));
			startRematch();
		}
		else {
			rematchRequestedLocally = true;
			updateRematchButton();
			sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.OFFER_REMATCH, localPlayerName, localColor, timeControl, increment));
		}
	}

	void opponentOfferRematch() {
		if(!gameover || rematchStarted) {
			return;
		}
		rematchRequestedByOpponent = true;
		// click race condition, if both requested rematch its accepted
		if(rematchRequestedLocally) {
			sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.ACCEPT_REMATCH, localPlayerName, localColor, timeControl, increment));
			startRematch();
			return;
		}
		if(gameoverDialog == null || !gameoverDialog.isVisible()) {
			showGameoverPopup();
		} else {
			updateRematchButton();
		}
	}

	void opponentAcceptRematch() {
		if(!gameover || rematchStarted || !rematchRequestedLocally) {
			return;
		}
		startRematch();
	}

	private void showGameoverPopup() {
		closeGameoverPopup();
		JButton rematch = new JButton("Revanche");
		JButton close = new JButton("Fechar");
		rematchButton = rematch;

		rematch.addActionListener(e -> rematchButtonClicked());
		close.addActionListener(e -> closeGameoverPopup());

		Object[] options = {rematch, close};
		JOptionPane pane = new JOptionPane(getGameResultMessage(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, rematch);
		JDialog dialog = pane.createDialog(this, "Fim de Jogo");
		dialog.setModal(false);
		gameoverDialog = dialog;
		updateRematchButton();
		dialog.setVisible(true);
	}

	private void drawBoardScene(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Board
		board.draw(g2, isBoardFlipped());
		
		if(checkedKing != null && historyIndex == moveHistory.size() - 1) {
			int kingX = toScreenCol(checkedKing.preCol) * Board.SQUARE_SIZE;
			int kingY = toScreenRow(checkedKing.preRow) * Board.SQUARE_SIZE;
			float centerX = kingX + Board.HALF_SQUARE_SIZE;
		    float centerY = kingY + Board.HALF_SQUARE_SIZE;
		    float radius = Board.SQUARE_SIZE * 0.70f;
		    Point2D center = new Point2D.Float(centerX, centerY);
		    float[] dist = {0.0f, 0.25f, 0.89f, 1.0f};
			Color[] gradientColors = {
					new Color(255, 0, 0, 255),
			        new Color(231, 0, 0, 255),
			        new Color(169, 0, 0, 0),
			        new Color(158, 0, 0, 0)
		    };
			RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, gradientColors);
			g2.setPaint(rgp);
			g2.fillRect(kingX, kingY, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
		}
		
		// Pieces
		if(historyIndex < moveHistory.size() - 1) {
			for(MoveRecord.PieceSnapshot s: moveHistory.get(historyIndex).boardState) {
				g2.drawImage(s.image, toScreenCol(s.col) * Board.SQUARE_SIZE, toScreenRow(s.row) * Board.SQUARE_SIZE, null);
			}
		}
		else {
			ArrayList<Piece> piecesToDraw = new ArrayList<>(pieces);
			for(int i=0; i<piecesToDraw.size(); i++) {
				Piece p = piecesToDraw.get(i);
				if (p == activePiece && mouse.pressed) {
					continue;
				}
				g2.drawImage(p.image, toScreenCol(p.col) * Board.SQUARE_SIZE, toScreenRow(p.row) * Board.SQUARE_SIZE, null);
			}
		}
		
		if(gameover && historyIndex == moveHistory.size() - 1) {
			if(stalemate) {
				int firstKingX = toScreenCol(getKing(true).preCol) * Board.SQUARE_SIZE;
				int firstKingY = toScreenRow(getKing(true).preRow) * Board.SQUARE_SIZE;
				
				int secondKingX = toScreenCol(getKing(false).preCol) * Board.SQUARE_SIZE;
				int secondKingY = toScreenRow(getKing(false).preRow) * Board.SQUARE_SIZE;
				
				g2.setColor(Color.decode("#74ADD5"));
				g2.fillOval(firstKingX + Board.SQUARE_SIZE - 40, firstKingY, 40, 40);
				g2.fillOval(secondKingX + Board.SQUARE_SIZE - 40, secondKingY, 40, 40);
				
				g2.setColor(Color.white);
				g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
				g2.drawString("🔒", firstKingX + Board.SQUARE_SIZE - 35, firstKingY + 25);
				g2.drawString("🔒", secondKingX + Board.SQUARE_SIZE - 35, secondKingY + 25);
			}
			else if(checkmate){
				int kingX = toScreenCol(checkedKing.preCol) * Board.SQUARE_SIZE;
				int kingY = toScreenRow(checkedKing.preRow) * Board.SQUARE_SIZE;
				
				g2.setColor(Color.red);
				g2.fillOval(kingX + Board.SQUARE_SIZE - 40, kingY, 40, 40);
				
				g2.setColor(Color.white);
				g2.setFont(new Font("Arial", Font.BOLD, 30));
				g2.drawString("#", kingX + Board.SQUARE_SIZE - 30, kingY + 30);
			}
			else if(calledResign) {
			    Piece resigningKing = getKing(false);
			    int kingX = toScreenCol(resigningKing.preCol) * Board.SQUARE_SIZE;
			    int kingY = toScreenRow(resigningKing.preRow) * Board.SQUARE_SIZE;
			    
			    g2.setColor(Color.red);
				g2.fillOval(kingX + Board.SQUARE_SIZE - 40, kingY, 40, 40);
				
				g2.setColor(Color.white);
				g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 30));
				g2.drawString("🏴", kingX + Board.SQUARE_SIZE - 30, kingY + 30);
			}
			else if(calledDraw || insufficientMaterial || drawByRepetition) {
			    Piece firstKing = getKing(true);
			    Piece secondKing = getKing(false);
			    
			    int firstKingX = toScreenCol(firstKing.preCol) * Board.SQUARE_SIZE;
			    int firstKingY = toScreenRow(firstKing.preRow) * Board.SQUARE_SIZE;
			    int secondKingX = toScreenCol(secondKing.preCol) * Board.SQUARE_SIZE;
			    int secondKingY = toScreenRow(secondKing.preRow) * Board.SQUARE_SIZE;
			    
			    g2.setColor(Color.decode("#74ADD5"));
				g2.fillOval(firstKingX + Board.SQUARE_SIZE - 40, firstKingY, 40, 40);
				g2.fillOval(secondKingX + Board.SQUARE_SIZE - 40, secondKingY, 40, 40);
				
				g2.setColor(Color.white);
				g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
				g2.drawString("½", firstKingX + Board.SQUARE_SIZE - 35, firstKingY + 25);
				g2.drawString("½", secondKingX + Board.SQUARE_SIZE - 35, secondKingY + 25);
			}
			else if(timeout) {
			    Piece flaggedKing = getKing(false);
			    int kingX = toScreenCol(flaggedKing.preCol) * Board.SQUARE_SIZE;
			    int kingY = toScreenRow(flaggedKing.preRow) * Board.SQUARE_SIZE;
			    
				g2.setColor(Color.white);
				g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
			    g2.drawString("⏱", kingX + Board.SQUARE_SIZE - 30, kingY + 30);
			}
		}
		
		// Promotion
		if(promotion && historyIndex == moveHistory.size() - 1) {
			g2.setColor(Color.black);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g2.fillRect(0, 0, 8 * Board.SQUARE_SIZE, 8 * Board.SQUARE_SIZE);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			
			int hoverCol = toBoardCol(mouse.x);
			int hoverRow = toBoardRow(mouse.y);
			for(int i=0; i<promotionPieces.size(); i++) {
				Piece p = promotionPieces.get(i);
				int px = toScreenCol(p.col) * Board.SQUARE_SIZE;
				int py = toScreenRow(p.row) * Board.SQUARE_SIZE;
				
				float centerX = px + Board.HALF_SQUARE_SIZE;
			    float centerY = py + Board.HALF_SQUARE_SIZE;
			    float radius = Board.HALF_SQUARE_SIZE;
			    
			    boolean isHovered = (hoverCol == p.col && hoverRow == p.row);
			    
			    Point2D center = new Point2D.Float(centerX, centerY);
			    float[] dist = {0.0f, 0.7f, 1.0f};
			    int promoPieceSize = 80;
			    if(isHovered) {
			    	Color[] gradientColors = {
			    		new Color(245, 195, 180),
			    		new Color(245, 150, 120),
			    		new Color(245, 110, 70)
			    		
			    	};
			    	promoPieceSize = 100;
			    	RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, gradientColors);
			    	g2.setPaint(rgp);
					g2.fillRect(px, py, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
			    }
			    else {
			    	Color[] gradientColors = {
				    		new Color(235, 235, 235),
				    		new Color(200, 200, 200),
				    		new Color(165, 165, 165)
				    };
			    	RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, gradientColors);
				    g2.setPaint(rgp);
					g2.fillOval(px, py, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
			    }
				
				int offset = (Board.SQUARE_SIZE - promoPieceSize) / 2;
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(p.image, px + offset, py + offset, promoPieceSize, promoPieceSize, null);
			}
		}
		else if(historyIndex == moveHistory.size() - 1){
			// Active piece
			if(activePiece != null) {
				int preX = toScreenCol(activePiece.preCol) * Board.SQUARE_SIZE;
				int preY = toScreenRow(activePiece.preRow) * Board.SQUARE_SIZE;
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g2.drawImage(activePiece.image, preX, preY, null);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g2.setColor(Color.decode("#245228"));
				g2.fillRect(preX, preY, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
				g2.setColor(Color.decode("#47914e"));
				ArrayList<int[]> movesToDraw = new ArrayList<>(validMoves);
				for (int[] move: movesToDraw) {
				    int i = move[0];
				    int j = move[1];
				    int screenCol = toScreenCol(i);
				    int screenRow = toScreenRow(j);
				    boolean isCapture = false;
				    for(Piece p: pieces) {
				        if(p.col == i && p.row == j && p != activePiece) {
				            isCapture = true;
				            break;
				        }
				    }

				    if(!isCapture) {
				        if(!(i == activePiece.col && j == activePiece.row)) {
				            g2.fillOval(screenCol * Board.SQUARE_SIZE + Board.HALF_SQUARE_SIZE - 15, 
				                        screenRow * Board.SQUARE_SIZE + Board.HALF_SQUARE_SIZE - 15, 30, 30);
				        }
				    }
				    else {
				        Area region = new Area(new Rectangle2D.Double(screenCol * Board.SQUARE_SIZE, 
				        		screenRow * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE));
				        Area hole = new Area(new Ellipse2D.Double(screenCol * Board.SQUARE_SIZE + Board.HALF_SQUARE_SIZE - 55, 
				        		screenRow * Board.SQUARE_SIZE + Board.HALF_SQUARE_SIZE - 55, 110, 110));
				        region.subtract(hole);
				        g2.fill(region);
				    }
				}
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				
				if(validMove) {
					g2.setColor(Color.decode("#47914e"));
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
					g2.fillRect(toScreenCol(activePiece.col) * Board.SQUARE_SIZE, toScreenRow(activePiece.row) * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				if(mouse.pressed) {
					activePiece.draw(g2);
				}
				else {
					g2.drawImage(activePiece.image, toScreenCol(activePiece.col) * Board.SQUARE_SIZE, toScreenRow(activePiece.row) * Board.SQUARE_SIZE, null);
				}
			}
		}
		Toolkit.getDefaultToolkit().sync();
	}
}

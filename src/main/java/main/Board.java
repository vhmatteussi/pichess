package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;

public class Board {
	public static final int SQUARE_SIZE = 100;
	public static final int HALF_SQUARE_SIZE = SQUARE_SIZE / 2;
	
	String whiteSquareColor = "#F0D9B5";
	String blackSquareColor = "#B58863";
	
	public void draw(Graphics2D g2) {
		draw(g2, false);
	}

	public void draw(Graphics2D g2, boolean flipped) {
		for(int r=0; r<8; r++) {
			for(int c=0; c<8; c++) {
				if((r + c) % 2 == 0) {
					g2.setColor(Color.decode(whiteSquareColor));
				}
				else {
					g2.setColor(Color.decode(blackSquareColor));
				}
				g2.fillRect(c*SQUARE_SIZE, r*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
			}
		}
		g2.setFont(new Font("Arial", Font.BOLD, 20));
		for(int c=0; c<8; c++) {
			if(c % 2 == 0) {
				g2.setColor(Color.decode(whiteSquareColor));
			}
			else {
				g2.setColor(Color.decode(blackSquareColor));
			}
			if(flipped) {
				g2.drawString(String.valueOf((char)('h' - c)), c * SQUARE_SIZE + 6, 8 * SQUARE_SIZE - 8);
			}
			else {
				g2.drawString(String.valueOf((char)('a' + c)), c * SQUARE_SIZE + 6, 8 * SQUARE_SIZE - 8);
			}
		}
		for(int r=0; r<8; r++) {
			if(r % 2 == 0) {
				g2.setColor(Color.decode(whiteSquareColor));
			}
			else {
				g2.setColor(Color.decode(blackSquareColor));
			}
			if(flipped) {
				g2.drawString(String.valueOf((char)('1' + r)), 8 * SQUARE_SIZE - 16, r * SQUARE_SIZE + 18);
			}
			else {
				g2.drawString(String.valueOf((char)('8' - r)), 8 * SQUARE_SIZE - 16, r * SQUARE_SIZE + 18);
			}
		}
	}
}

package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import pitiupi.presentation.shell.ui.ShellUi;

public class SidebarRenderer extends JPanel {
    private static final int SIDEBAR_WIDTH = 300;
    private static final int LOW_TIME_THRESHOLD_MS = 10000;

    private final GamePanel gp;

    private final JLabel blackNameLabel = new JLabel();
    private final JLabel blackStatusLabel = new JLabel();
    private final JLabel blackClockLabel = new JLabel();
    private final JLabel whiteNameLabel = new JLabel();
    private final JLabel whiteStatusLabel = new JLabel();
    private final JLabel whiteClockLabel = new JLabel();

    private final DefaultTableModel historyModel;
    private final JTable historyTable;

    private final JButton drawButton = new JButton("Empate");
    private final JButton resignButton = new JButton("Desistir");

    private int lastSyncedMoveCount = -1;

    public SidebarRenderer(GamePanel gp) {
        this.gp = gp;

        setPreferredSize(new Dimension(SIDEBAR_WIDTH, GamePanel.HEIGHT));
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 1, 0, 0, ShellUi.shellEdgeColor()), new EmptyBorder(12, 12, 12, 12)));

        // black player and clock
        JPanel topPanel = createPlayerPanel(blackClockLabel, blackNameLabel, blackStatusLabel, true);
        add(topPanel, BorderLayout.NORTH);

        // move history table
        historyModel = new DefaultTableModel(new String[] { "#", "Brancas", "Pretas" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyModel);
        historyTable.setFocusable(false);
        historyTable.setRowSelectionAllowed(false);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(45);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        add(historyScroll, BorderLayout.CENTER);

        // action buttons, white player and white clock
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        drawButton.setFocusable(false);
        resignButton.setFocusable(false);
        drawButton.addActionListener(e -> gp.drawButtonClicked());
        resignButton.addActionListener(e -> gp.resignButtonClicked());
        buttonsPanel.add(drawButton);
        buttonsPanel.add(resignButton);

        JPanel bottomPlayerPanel = createPlayerPanel(whiteClockLabel, whiteNameLabel, whiteStatusLabel, false);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        bottomPanel.add(bottomPlayerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel createPlayerPanel(JLabel clockLabel, JLabel nameLabel, JLabel statusLabel, boolean clockOnTop) {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ShellUi.shellEdgeColor()), new EmptyBorder(8, 10, 8, 10)));

        clockLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        clockLabel.setHorizontalAlignment(SwingConstants.LEFT);

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);

        statusLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel nameRow = new JPanel(new BorderLayout(6, 0));
        nameRow.setOpaque(false);
        nameRow.add(nameLabel, BorderLayout.CENTER);
        nameRow.add(statusLabel, BorderLayout.EAST);

        if(clockOnTop) {
            panel.add(clockLabel);
            panel.add(nameRow);
        }
        else {
            panel.add(nameRow);
            panel.add(clockLabel);
        }
        return panel;
    }

    public String formatTime(long millis) {
        if(millis < 0) {
            millis = 0;
        }

        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        // show milisseconds if the clock is low on time
        if(millis < LOW_TIME_THRESHOLD_MS) {
            long tenths = (millis % 1000) / 100;
            return String.format("%d:%02d.%d", minutes, seconds, tenths);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void refresh() {
        boolean isBlackTurn = (gp.currentColor == GamePanel.BLACK && !gp.gameover);
        boolean isWhiteTurn = (gp.currentColor == GamePanel.WHITE && !gp.gameover);

        if(gp.isBoardFlipped()) {
            updatePlayerSection(blackNameLabel, blackStatusLabel, blackClockLabel, gp.firstPlayerName, gp.whiteConnected, gp.whiteTime, isWhiteTurn);
            updatePlayerSection(whiteNameLabel, whiteStatusLabel, whiteClockLabel, gp.secondPlayerName, gp.blackConnected, gp.blackTime, isBlackTurn);
        }
        else {
            updatePlayerSection(blackNameLabel, blackStatusLabel, blackClockLabel, gp.secondPlayerName, gp.blackConnected, gp.blackTime, isBlackTurn);
            updatePlayerSection(whiteNameLabel, whiteStatusLabel, whiteClockLabel, gp.firstPlayerName, gp.whiteConnected, gp.whiteTime, isWhiteTurn);
        }

        drawButton.setEnabled(!gp.gameover);
        resignButton.setEnabled(!gp.gameover);

        if(gp.confirmDraw) {
            drawButton.setText("Confirmar");
        }
        else {
            drawButton.setText("Empate");
        }

        if(gp.confirmResign) {
            drawButton.setText("Confirmar");
        }
        else {
            drawButton.setText("Desistir");
        }

        syncMoveHistory();
    }

    private void updatePlayerSection(JLabel nameLabel, JLabel statusLabel, JLabel clockLabel, String playerName, boolean connected, long timeMillis, boolean isActive) {
        if(isActive) {
            nameLabel.setText("> ");
        }
        else {
            nameLabel.setText("" + playerName);
        }

        if(connected) {
            // full circle
            statusLabel.setText("\u25CF Conectado");
            statusLabel.setForeground(new Color(46, 160, 67));
        }
        else {
            // empty circle
            statusLabel.setText("\u25CB Desconectado");
            statusLabel.setForeground(new Color(200, 65, 65));
        }

        clockLabel.setText(formatTime(timeMillis));
        if(isActive && timeMillis < LOW_TIME_THRESHOLD_MS) {
            clockLabel.setForeground(new Color(190, 45, 45));
        }
        else if(isActive) {
            clockLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
        else {
            Color muted = UIManager.getColor("Label.disabledForeground");
            if(muted != null) {
                clockLabel.setForeground(muted);
            }
            else {
                clockLabel.setForeground(Color.gray);
            }
        }
    }

    private void syncMoveHistory() {
        int totalMoves = Math.max(0, gp.moveHistory.size() - 1);
        if(totalMoves == lastSyncedMoveCount) {
            return;
        }
        lastSyncedMoveCount = totalMoves;

        historyModel.setRowCount(0);
        int totalTurns = (totalMoves + 1) / 2;
        for(int t=0; t<totalTurns; t++) {
            int whiteIndex = t * 2 + 1;
            int blackIndex = t * 2 + 2;

            String whiteMove;
            if(whiteIndex < gp.moveHistory.size()) {
                whiteMove = gp.moveHistory.get(whiteIndex).notation;
            }
            else {
                whiteMove = "";
            }

            String blackMove;
            if(blackIndex < gp.moveHistory.size()) {
                blackMove = gp.moveHistory.get(blackIndex).notation;
            }
            else {
                blackMove = "";
            }

            historyModel.addRow(new Object[] {(t + 1) + ".", whiteMove, blackMove});
        }

        if(totalTurns > 0) {
            int lastRow = totalTurns - 1;
            historyTable.scrollRectToVisible(historyTable.getCellRect(lastRow, 0, true));
        }
    }
}

package main;

import java.net.InetAddress;
import java.util.function.BiConsumer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import main.CustomOptionPane.HostPlayerColor;
import messages.PichessActionMessage;
import messages.PichessMoveMessage;
import pitiupi.application.presence.UserPeerView;
import pitiupi.models.Messages.Message;

public class Network {
    private final GamePanel gp;

    public Network(GamePanel gp) {
        this.gp = gp;
    }

    public void setLocalPlayerName(String localPlayerName) {
        gp.localPlayerName = localPlayerName;
    }

    public void startNetworkGame(UserPeerView peer, int minutes, int increment, HostPlayerColor hostColor) {
        gp.timeControl = minutes * 60 * 1000;
        gp.increment = increment * 1000;
        gp.opponentAddress = peer.getInetAddress().orElse(null);
        gp.opponentName = peer.getDisplayName();
        gp.hostColor = hostColor;

        if(hostColor == HostPlayerColor.COLOR_WHITE) {
            gp.localColor = GamePanel.WHITE;
        }
        else if(hostColor == HostPlayerColor.COLOR_BLACK) {
            gp.localColor = GamePanel.BLACK;
        }
        else {
            if(java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) {
                gp.localColor = GamePanel.WHITE;
            }
            else {
                gp.localColor = GamePanel.BLACK;
            }
        }

        if(gp.localColor == GamePanel.WHITE) {
            gp.opponentColor = GamePanel.BLACK;
            gp.firstPlayerName = gp.localPlayerName;
            gp.secondPlayerName = gp.opponentName;
        }
        else {
            gp.opponentColor = GamePanel.WHITE;
            gp.firstPlayerName = gp.opponentName;
            gp.secondPlayerName = gp.localPlayerName;
        }

        gp.resetGameState();
        gp.matchAccepted = false;
        gp.updateConnection(true, true);

        sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.INVITE, gp.localPlayerName, 
            gp.localColor, gp.timeControl, gp.increment));
    }

    public void messageSender(BiConsumer<InetAddress, Message> messageSender) {
        gp.messageSender = messageSender;
    }

    public void sendMessage(Message message) {
        gp.messageSender.accept(gp.opponentAddress, message);
    }

    public void getMoveMessage(PichessMoveMessage moveMessage) {
        if(moveMessage == null || moveMessage.getLastMove() == null) {
            return;
        }
        gp.moveReceived = moveMessage.getLastMove();
        gp.moveListReceived = moveMessage.getMoveList();
        // se uma moveList for diferente da outra, teve algum problema
        if(!checkMoveList()) {
            gp.gameAborted = true;
            gp.gameover = true;
            return;
        }
        gp.whiteTime = gp.moveReceived.getWhiteTime();
        gp.blackTime = gp.moveReceived.getBlackTime();
    }

    private boolean checkMoveList() {
        if(gp.moveListReceived == null || gp.moveListReceived.size() != gp.localMoveList.size() + 1) {
            return false;
        }
        for(int i=0; i<gp.localMoveList.size(); i++) {
            if(!gp.localMoveList.get(i).equals(gp.moveListReceived.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void didGetInvite(PichessActionMessage actionMessage, InetAddress senderAddress){
        long minute = actionMessage.getTimeControl() / 60000;
        int increment = actionMessage.getIncrement() / 1000;
        String opponent = actionMessage.getPlayerName();

        String responseMessage = opponent + " convidou você para uma partida (" + minute + "+" + increment + ").";
        int response = JOptionPane.showConfirmDialog(gp, responseMessage, "Notificação Pichess", JOptionPane.YES_NO_OPTION);

        if(response == JOptionPane.YES_OPTION) {
            gp.timeControl = actionMessage.getTimeControl();
            gp.increment = actionMessage.getIncrement();
            gp.opponentColor = actionMessage.getPlayerColor();
            if(gp.opponentColor == GamePanel.WHITE){
                gp.localColor = GamePanel.BLACK;
            }
            else{
                gp.localColor = GamePanel.WHITE;
            }
            gp.opponentName = actionMessage.getPlayerName();
            if(gp.localColor == GamePanel.WHITE) {
                gp.firstPlayerName = gp.localPlayerName;
                gp.secondPlayerName = gp.opponentName;
            }
            else {
                gp.firstPlayerName = gp.opponentName;
                gp.secondPlayerName = gp.localPlayerName;
            }
            gp.resetGameState();
            gp.matchAccepted = true;
            gp.updateConnection(true, true);
            gp.launchGame();
            sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.ACCEPT_INVITE, gp.localPlayerName, 
                gp.localColor, gp.timeControl, gp.increment));
        }
        else {
            sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.DECLINE_INVITE, gp.localPlayerName, 
                gp.localColor, gp.timeControl, gp.increment));
        }
    }

    private void offeredDraw(PichessActionMessage actionMessage, InetAddress senderAddress) {
        int response = JOptionPane.showConfirmDialog(gp, 
            actionMessage.getPlayerName() + " propôs empate.", "Proposta de empate", JOptionPane.YES_NO_OPTION);
                if(response == JOptionPane.YES_OPTION) {
                    gp.calledDraw = true;
                    gp.gameover = true;
                    gp.activePiece = null;
                    sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.ACCEPT_DRAW, gp.localPlayerName, 
                        gp.localColor, gp.timeControl, gp.increment));
                }
                else {
                    sendMessage(new PichessActionMessage(PichessActionMessage.PichessActions.DECLINE_DRAW, gp.localPlayerName,
                        gp.localColor, gp.timeControl, gp.increment));
                }
    }

    public void getActionMessage(PichessActionMessage actionMessage, InetAddress senderAddress) {
        if(actionMessage == null || actionMessage.getAction() == null) {
            return;
        }
        gp.opponentAddress = senderAddress;
        gp.action = actionMessage.getAction();

        switch(actionMessage.getAction()) {
            case INVITE:
                didGetInvite(actionMessage, senderAddress);
                break;
            case ACCEPT_INVITE:
                gp.matchAccepted = true;
                gp.lastClockUpdate = System.currentTimeMillis();
                break;
            case DECLINE_INVITE:
                gp.matchAccepted = false;
                JOptionPane.showMessageDialog(gp, 
                    actionMessage.getPlayerName() + " recusou seu desafio.", "Notificação Pichess", JOptionPane.INFORMATION_MESSAGE);
                break;
            case RESIGN:
                gp.calledResign = true;
                gp.gameover = true;
                gp.activePiece = null;
                break;
            case OFFER_DRAW:
                offeredDraw(actionMessage, senderAddress);
                break;
            case ACCEPT_DRAW:
                gp.calledDraw = true;
                gp.gameover = true;
                gp.activePiece = null;
                break;
            case DECLINE_DRAW:
                JOptionPane.showMessageDialog(gp, 
                    actionMessage.getPlayerName() + " recusou o empate.", "Notificação Pichess", JOptionPane.INFORMATION_MESSAGE);
                break;
            case OFFER_REMATCH:
                SwingUtilities.invokeLater(gp::opponentOfferRematch);
                break;
            case ACCEPT_REMATCH:
                SwingUtilities.invokeLater(gp::opponentAcceptRematch);
                break;
            case DECLINE_REMATCH:
                SwingUtilities.invokeLater(() -> {
                    gp.rematchRequestedLocally = false;
                    gp.updateRematchButton();
                    JOptionPane.showMessageDialog(gp, 
                        actionMessage.getPlayerName() + " recusou a revanche.", "Notificação Pichess", JOptionPane.INFORMATION_MESSAGE);
                });
                break;
            default:
                break;
        }
    }
}

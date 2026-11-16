package plugin;

import java.awt.GridBagLayout;
import java.awt.event.HierarchyEvent;
import java.net.InetAddress;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import main.CustomOptionPane;
import main.GamePanel;
import messages.PichessActionMessage;
import messages.PichessMoveMessage;
import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.models.User;

public class PiChessPlugin extends Plugin{

    private GamePanel gamePanel;
    private PluginWorkspace workspace;
    private ConnectionManager connectionManager;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.gamePanel = new GamePanel();
        this.gamePanel.setLocalPlayerName(connectionManager.getUserName());
        this.gamePanel.messageSender(this::sendMessage);

        JPanel container = new JPanel(new GridBagLayout());
        container.add(gamePanel);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        scrollPane.addHierarchyListener(e -> {
            if((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if(scrollPane.isShowing()) {
                    gamePanel.launchGame();
                }
            }
        });

        bindShellWorkspaceContent(scrollPane);
        installPeerActions(workspace.peerActions());
        connectionManager.addPeerListener(peers -> SwingUtilities.invokeLater(() -> updateConnection(peers)));
        updateConnection(connectionManager.getHeartbeatManager().getPeers());
    }

    private void updateConnection(List<User> peers) {
        InetAddress opponentAddress = gamePanel.getOpponentAddress();
        String opponentName = gamePanel.getOpponentName();
        String localId = connectionManager.getHeartbeatManager().getInstanceId();

        boolean opponentOnline = false;
        if(opponentAddress != null && peers != null) {
            for(User peer: peers) {
                if(localId != null && localId.equals(peer.getInstanceId())) {
                    continue;
                }
                if(opponentName.equals(peer.getUserName()) && opponentAddress.equals(peer.getAddress())) {
                    opponentOnline = true;
                    break;
                }
            }
        }
        gamePanel.updateConnection(true, opponentOnline);
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "pichess.blitz.3.0",
                "Blitz: 3+0",
                PeerActionTarget.SINGLE_PEER,
                target -> startGame(target, 3, 0)
        ));
        registrar.register(PeerAction.of(
                "pichess.blitz.3.2",
                "Blitz: 3+2",
                PeerActionTarget.SINGLE_PEER,
                target -> startGame(target, 3, 2)
        ));
        registrar.register(PeerAction.of(
                "pichess.rapid.10.0",
                "Rápida: 10+0",
                PeerActionTarget.SINGLE_PEER,
                target -> startGame(target, 10, 0)
        ));
        registrar.register(PeerAction.of(
                "pichess.rapid.10.5",
                "Rápida: 10+5",
                PeerActionTarget.SINGLE_PEER,
                target -> startGame(target, 10, 5)
        ));
        registrar.register(PeerAction.of(
                "pichess.classical.30.0",
                "Clássica: 30+0",
                PeerActionTarget.SINGLE_PEER,
                target -> startGame(target, 30, 0)
        ));
        registrar.register(PeerAction.of(
                "pichess.custom.time.control",
                "Customizada",
                PeerActionTarget.SINGLE_PEER,
                target -> {
                    CustomOptionPane custom = new CustomOptionPane();
                    if(custom.isConfirmed()) {
                        startGame(target, custom.getMinutes(), custom.getIncrement(), custom.getHostColor());
                    }
                }
        ));
    }

    private void startGame(PeerTarget target, int minutes, int increment) {
        startGame(target, minutes, increment, CustomOptionPane.HostPlayerColor.COLOR_RANDOM);
    }

    private void startGame(PeerTarget target, int minutes, int increment, CustomOptionPane.HostPlayerColor hostColor) {
        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
        if(target instanceof PeerTarget.Single single) {
            gamePanel.startNetworkGame(single.peer(), minutes, increment, hostColor);
        }
    }

    @Override
    public void onShellTabClosed() {
        gamePanel.stopGame();
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(PichessMoveMessage.class, PichessActionMessage.class);
    }

    private void sendMessage(InetAddress target, Message message) {
        MessageDispatcher.send(connectionManager, message, SendTarget.singlePeer(target), TransportMode.TCP);
    }

    @Override
    public void receiveMessage(Message message) {
        if(message instanceof PichessMoveMessage moveMessage) {
            SwingUtilities.invokeLater(() -> gamePanel.getMoveMessage(moveMessage));
        }
        else if(message instanceof PichessActionMessage actionMessage) {
            InetAddress senderAddress = getPeerAddress(actionMessage.getPlayerName());
            SwingUtilities.invokeLater(() -> {
                // if its an invite, open game frame
                if(actionMessage.getAction() == PichessActionMessage.PichessActions.INVITE) {
                    workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                }
                gamePanel.getActionMessage(actionMessage, senderAddress);
            });
        }
    }

    private InetAddress getPeerAddress(String playerName) {
        for(UserPeerView peer: connectionManager.getPresenceService().listPeerViews()) {
            if(!peer.isSelf() && !peer.isBroadcastTarget() && playerName.equals(peer.getDisplayName())) {
                return peer.getInetAddress().orElse(null);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Pichess";
    }

    @Override
    public String getDisplayName() {
        return "Pichess";
    }

    @Override
    public String getAuthor() {
        return "Victor Hugo";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.Pichess";
    }
}

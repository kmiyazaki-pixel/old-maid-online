import javax.swing.*;
import java.awt.*;
import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.util.*;
import java.util.List;

public class OldMaidApp extends JFrame {
    private List<String> myCards = new ArrayList<>();
    private JPanel cardPanel;
    private JLabel statusLabel;
    private MyWebSocketClient client;
    private boolean myTurn = false;

    // あなたのRenderのURL（ws:// はWebSocket通信の合図です）
    private static final String SERVER_URL = "wss://old-maid-online.onrender.com";

    public OldMaidApp() {
        setTitle("Old Maid Online - Web Edition");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        statusLabel = new JLabel("サーバーに接続中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        cardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        cardPanel.setBackground(new Color(30, 120, 60));

        add(statusLabel, BorderLayout.NORTH);
        add(new JScrollPane(cardPanel), BorderLayout.CENTER);

        connectToServer();
    }

    private void connectToServer() {
        try {
            client = new MyWebSocketClient(new URI(SERVER_URL));
            client.connect();
        } catch (Exception e) {
            statusLabel.setText("接続エラー: " + e.getMessage());
        }
    }

    // 内部クラス：WebSocketのイベントを処理
    class MyWebSocketClient extends WebSocketClient {
        public MyWebSocketClient(URI serverUri) { super(serverUri); }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("接続成功！対戦相手を待っています..."));
        }

        @Override
        public void onMessage(String message) {
            SwingUtilities.invokeLater(() -> {
                if (message.startsWith("CARD_ADD:")) {
                    myCards.add(message.substring(9));
                    refreshUI();
                } else if (message.equals("YOUR_TURN")) {
                    myTurn = true;
                    statusLabel.setText("あなたの番です！");
                } else if (message.startsWith("INFO:")) {
                    statusLabel.setText(message.substring(5));
                } else if (message.startsWith("DRAW_ACTION:")) {
                    myTurn = false;
                    removePairs();
                    refreshUI();
                }
            });
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("切断されました"));
        }

        @Override
        public void onError(Exception ex) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("エラー: " + ex.getMessage()));
        }
    }

    private void removePairs() {
        Map<String, Integer> counts = new HashMap<>();
        List<String> nextCards = new ArrayList<>();
        for (String c : myCards) {
            if (c.equals("J0")) { nextCards.add(c); continue; }
            String rank = c.substring(1);
            if (counts.containsKey(rank)) {
                counts.remove(rank);
                nextCards.removeIf(card -> card.length() > 1 && card.substring(1).equals(rank));
            } else {
                counts.put(rank, 1);
                nextCards.add(c);
            }
        }
        myCards = nextCards;
    }

    private void refreshUI() {
        cardPanel.removeAll();
        for (String code : myCards) {
            JButton btn = new JButton(formatCard(code));
            btn.setPreferredSize(new Dimension(70, 100));
            btn.setFont(new Font("Serif", Font.BOLD, 18));
            if (code.startsWith("H") || code.startsWith("D")) btn.setForeground(Color.RED);
            btn.addActionListener(e -> {
                if (myTurn) client.send("DRAW_REQUEST:0");
            });
            cardPanel.add(btn);
        }
        cardPanel.revalidate(); cardPanel.repaint();
    }

    private String formatCard(String code) {
        if (code.equals("J0")) return "JKR";
        return code.replace("S","♠").replace("H","♥").replace("D","♦").replace("C","♣");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OldMaidApp().setVisible(true));
    }
}

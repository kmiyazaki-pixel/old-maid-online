import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class OldMaidApp extends JFrame {
    private Hand myHand = new Hand();
    private List<Card> cardsInHand = new ArrayList<>();
    private JPanel cardPanel;
    private JLabel statusLabel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public OldMaidApp() {
        setTitle("Old Maid Online - Player");
        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // UIの組み立て
        JPanel mainPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("サーバーに接続中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        
        cardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        cardPanel.setBackground(new Color(20, 100, 40)); // 緑のテーブル風

        mainPanel.add(statusLabel, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);
        add(mainPanel);

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                // サーバーに接続
                socket = new Socket("localhost", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("CARD:")) {
                        // サーバーからカードが届いた（例: CARD:S1）
                        String cardCode = line.substring(5);
                        addCardToHand(cardCode);
                    } else if (line.startsWith("GAME_START:")) {
                        // ゲーム開始の合図
                        String msg = line.substring(11);
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(msg);
                            refreshHandUI();
                        });
                    } else {
                        // その他のメッセージ（人数通知など）
                        String msg = line;
                        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("接続エラー: サーバーを先に起動してください"));
            }
        }).start();
    }

    private void addCardToHand(String code) {
        // 簡易的なカード表現（実際はCardクラスに変換してもOK）
        cardsInHand.add(new Card(code));
    }

    private void refreshHandUI() {
        cardPanel.removeAll();
        for (Card c : cardsInHand) {
            JLabel label = new JLabel(c.getDisplayName());
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setPreferredSize(new Dimension(60, 90));
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (c.isRed()) label.setForeground(Color.RED);
            cardPanel.add(label);
        }
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // --- 内部用簡易Cardクラス ---
    class Card {
        String code;
        Card(String code) { this.code = code; }
        String getDisplayName() {
            if (code.equals("J0")) return "JOKER";
            String suit = code.substring(0, 1);
            String rank = code.substring(1);
            String sSymbol = suit.replace("S","♠").replace("H","♥").replace("D","♦").replace("C","♣");
            return sSymbol + rank;
        }
        boolean isRed() { return code.startsWith("H") || code.startsWith("D"); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OldMaidApp().setVisible(true));
    }
}

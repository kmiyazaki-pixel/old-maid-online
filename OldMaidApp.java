import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class OldMaidApp extends JFrame {
    private List<String> myCards = new ArrayList<>();
    private JPanel cardPanel;
    private JLabel statusLabel;
    private PrintWriter out;
    private boolean myTurn = false;

    public OldMaidApp() {
        setTitle("Old Maid Online");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        statusLabel = new JLabel("相手の接続を待っています...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        cardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        cardPanel.setBackground(new Color(30, 120, 60));

        add(statusLabel, BorderLayout.NORTH);
        add(new JScrollPane(cardPanel), BorderLayout.CENTER);

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("CARD_ADD:")) {
                        myCards.add(line.substring(9));
                        refreshUI();
                    } else if (line.equals("YOUR_TURN")) {
                        myTurn = true;
                        statusLabel.setText("あなたの番です！相手のカードを選んでください");
                    } else if (line.startsWith("INFO:")) {
                        statusLabel.setText(line.substring(5));
                    } else if (line.startsWith("DRAW_ACTION:")) {
                        myTurn = false;
                        statusLabel.setText("カードが移動しました...");
                        // ここでペア削除ロジックを走らせる
                        removePairs();
                        refreshUI();
                    }
                }
            } catch (IOException e) { statusLabel.setText("サーバー未起動"); }
        }).start();
    }

    private void removePairs() {
        Map<String, Integer> counts = new HashMap<>();
        List<String> nextCards = new ArrayList<>();
        // 数字(Rank)だけでカウント
        for (String c : myCards) {
            if (c.equals("J0")) { nextCards.add(c); continue; }
            String rank = c.substring(1);
            if (counts.containsKey(rank)) {
                counts.remove(rank);
                nextCards.removeIf(card -> card.substring(1).equals(rank));
            } else {
                counts.put(rank, 1);
                nextCards.add(c);
            }
        }
        myCards = nextCards;
    }

    private void refreshUI() {
        SwingUtilities.invokeLater(() -> {
            cardPanel.removeAll();
            for (String code : myCards) {
                JButton btn = new JButton(formatCard(code));
                btn.setPreferredSize(new Dimension(70, 100));
                btn.setFont(new Font("Serif", Font.BOLD, 18));
                if (code.startsWith("H") || code.startsWith("D")) btn.setForeground(Color.RED);
                btn.addActionListener(e -> {
                    if (myTurn) out.println("DRAW_REQUEST:0");
                });
                cardPanel.add(btn);
            }
            cardPanel.revalidate(); cardPanel.repaint();
        });
    }

    private String formatCard(String code) {
        if (code.equals("J0")) return "JKR";
        return code.replace("S","♠").replace("H","♥").replace("D","♦").replace("C","♣");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OldMaidApp().setVisible(true));
    }
}

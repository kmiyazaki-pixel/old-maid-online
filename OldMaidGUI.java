// --- インポート文の最後に追加 ---
import java.io.*;
import java.net.*;

public class OldMaidGUI extends JFrame {
    // --- 既存の変数の下に追加 ---
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public OldMaidGUI() {
        // ...既存のコード...
        setupUI();
        
        // --- コンストラクタの最後に追加 ---
        connectToServer(); 
        
        currentPlayerIdx = 0;
        nextTurn();
    }

    // --- 新しいメソッドとして追加 ---
    private void connectToServer() {
        new Thread(() -> {
            try {
                this.socket = new Socket("localhost", 12345);
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String msg;
                while ((msg = in.readLine()) != null) {
                    final String serverMsg = msg;
                    // サーバーからのメッセージを画面上のラベルに表示
                    SwingUtilities.invokeLater(() -> statusLabel.setText(serverMsg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("サーバー未接続（オフライン）"));
            }
        }).start();
    }


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;       
import java.util.HashMap;  
import java.util.Collections;
import java.util.EnumSet;

public class OldMaidGUI extends JFrame {
    private List<Hand> allHands;
    private List<CardPanel> cpuPanels;
    private CardPanel playerPanel;
    private JLabel statusLabel;
    private int playerCount = 2;
    private int currentPlayerIdx = 0; 
    private boolean isPlayerTurn = false;

    public OldMaidGUI() {
        setTitle("Supreme Old Maid - Luxury Multi-Edition");
        setSize(1200, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 90, 45), 0, getHeight(), new Color(10, 40, 20));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(180, 150, 50, 150));
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(15, 15, getWidth()-30, getHeight()-30);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(22, 22, getWidth()-44, getHeight()-44);
            }
        };
        setContentPane(contentPane);

        selectPlayerCount();
    }

    private void selectPlayerCount() {
        Integer[] options = {2, 3, 4, 5, 6};
        Integer res = (Integer) JOptionPane.showInputDialog(this, 
            "対戦人数（2～6人）を選んでください", "Game Start",
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (res == null) System.exit(0);
        playerCount = res;
        initGame();
    }

    private void initGame() {
        allHands = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) allHands.add(new Hand());
        Deck deck = new Deck();
        int turn = 0;
        while (true) {
            Card c = deck.draw();
            if (c == null) break;
            allHands.get(turn % playerCount).addCard(c);
            turn++;
        }
        for (Hand h : allHands) h.discardPairs();
        
        setupUI();
        currentPlayerIdx = 0;
        nextTurn();
    }

    private void setupUI() {
        getContentPane().removeAll();
        cpuPanels = new ArrayList<>();

        JPanel topArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topArea.setOpaque(false);
        for (int i = 1; i < playerCount; i++) {
            CardPanel cp = new CardPanel(allHands.get(i), true, "CPU " + i);
            cpuPanels.add(cp);
            topArea.add(cp);
        }

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 255, 200));
        statusLabel.setFont(new Font("Serif", Font.ITALIC, 28));

        playerPanel = new CardPanel(allHands.get(0), false, "YOU");

        add(topArea, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);

        revalidate(); repaint();
    }

    private void nextTurn() {
        if (allHands.get(currentPlayerIdx).size() == 0) {
            passTurn();
            return;
        }

        if (currentPlayerIdx == 0) {
            isPlayerTurn = true;
            statusLabel.setText("あなたの番です。CPU 1 のカードを引いてください");
        } else {
            isPlayerTurn = false;
            startCpuAi(currentPlayerIdx);
        }
    }

    private void startCpuAi(int idx) {
        int target = (idx + 1) % playerCount;
        while (allHands.get(target).size() == 0) target = (target + 1) % playerCount;

        statusLabel.setText("CPU " + idx + " が考え中...");
        int finalTarget = target;
        Timer t = new Timer(1500, e -> {
            int cardIdx = (int)(Math.random() * allHands.get(finalTarget).size());
            executeDraw(idx, finalTarget, cardIdx);
        });
        t.setRepeats(false); t.start();
    }

    private void executeDraw(int drawer, int victim, int cardIdx) {
        Card picked = allHands.get(victim).pullCard(cardIdx);
        allHands.get(drawer).addCard(picked);
        statusLabel.setText((drawer==0?"あなた":"CPU "+drawer) + " がカードを引きました");
        repaint();

        Timer t = new Timer(1000, e -> {
            allHands.get(drawer).discardPairs();
            repaint();
            if (!checkGameOver()) passTurn();
        });
        t.setRepeats(false); t.start();
    }

    private void passTurn() {
        currentPlayerIdx = (currentPlayerIdx + 1) % playerCount;
        nextTurn();
    }

    private boolean checkGameOver() {
        int active = 0;
        for (Hand h : allHands) if (h.size() > 0) active++;
        if (active <= 1) {
            String result = allHands.get(0).size() == 0 ? "勝利！" : "敗北...";
            JOptionPane.showMessageDialog(this, "ゲーム終了！ あなたは " + result);
            selectPlayerCount();
            return true;
        }
        return false;
    }

    class CardPanel extends JPanel {
        private Hand hand;
        private boolean isCPU;
        private String name;
        private final int CARD_W = 90;
        private final int CARD_H = 130;

        public CardPanel(Hand hand, boolean isCPU, String name) {
            this.hand = hand; this.isCPU = isCPU; this.name = name;
            setOpaque(false);
            setPreferredSize(new Dimension(220, 200));
            if (isCPU) {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (isPlayerTurn && name.equals("CPU 1")) {
                            int idx = getCardIndexAt(e.getPoint());
                            if (idx != -1) executeDraw(0, 1, idx);
                        }
                    }
                });
            }
        }

        public int getCardIndexAt(Point p) {
            int size = hand.size();
            int startX = (getWidth() - (size * 25 + 65)) / 2;
            for (int i = size - 1; i >= 0; i--) {
                if (new Rectangle(startX + i * 25, 40, CARD_W, CARD_H).contains(p)) return i;
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Serif", Font.BOLD, 16));
            g2.drawString(name, getWidth()/2 - 20, 25);

            List<Card> cards = hand.getCards();
            int startX = (getWidth() - (cards.size() * 25 + 65)) / 2;
            for (int i = 0; i < cards.size(); i++) {
                int x = startX + i * 25; int y = 40;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(x+3, y+3, CARD_W, CARD_H, 15, 15);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x, y, CARD_W, CARD_H, 15, 15);
                g2.setColor(new Color(180, 180, 180));
                g2.drawRoundRect(x, y, CARD_W, CARD_H, 15, 15);

                if (isCPU) drawBack(g2, x, y);
                else drawFront(g2, cards.get(i), x, y);
            }
        }

        private void drawBack(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(30, 60, 140));
            g2.fillRoundRect(x+5, y+5, CARD_W-10, CARD_H-10, 10, 10);
            g2.setColor(new Color(255, 215, 0, 80));
            g2.drawRect(x+10, y+10, CARD_W-20, CARD_H-20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Serif", Font.BOLD, 18));
            g2.drawString("VS", x + 33, y + 70);
        }

        private void drawFront(Graphics2D g2, Card card, int x, int y) {
            g2.setColor(card.getColor());
            String rank = switch(card.getRank()){case 1->"A";case 11->"J";case 12->"Q";case 13->"K";default->""+card.getRank();};
            String suit = switch(card.getSuit()){case SPADE->"♠";case HEART->"♥";case DIAMOND->"♦";case CLUB->"♣";default->"";};
            
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString(rank, x + 8, y + 22);
            if (card.isJoker()) {
                g2.setColor(new Color(150, 0, 150));
                g2.setFont(new Font("Serif", Font.PLAIN, 50));
                g2.drawString("🤡", x + 18, y + 85);
            } else {
                g2.setFont(new Font("Serif", Font.PLAIN, 45));
                g2.drawString(suit, x + 22, y + 85);
            }
        }
    } // CardPanelの終わり

class Card {
    enum Suit { SPADE, HEART, DIAMOND, CLUB, JOKER }
    private final Suit suit;
    private final int rank; // 1-13, Jokerは0

    public Card(Suit suit, int rank) { this.suit = suit; this.rank = rank; }
    public Suit getSuit() { return suit; }
    public int getRank() { return rank; }
    public boolean isJoker() { return suit == Suit.JOKER; }
    public Color getColor() {
        return (suit == Suit.HEART || suit == Suit.DIAMOND) ? Color.RED : Color.BLACK;
    }
}

class Hand {
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card c) { cards.add(c); }
    public int size() { return cards.size(); }
    public List<Card> getCards() { return cards; }

    public Card pullCard(int idx) { return cards.remove(idx); }

    // ペア（同じ数字）を捨てるロジック
    public void discardPairs() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : cards) {
            if (!c.isJoker()) counts.put(c.getRank(), counts.getOrDefault(c.getRank(), 0) + 1);
        }
        for (Integer rank : counts.keySet()) {
            int pairs = counts.get(rank) / 2;
            for (int i = 0; i < pairs * 2; i++) {
                for (int j = 0; j < cards.size(); j++) {
                    if (!cards.get(j).isJoker() && cards.get(j).getRank() == rank) {
                        cards.remove(j);
                        break;
                    }
                }
            }
        }
    }
}


class Deck {
    private List<Card> cards = new ArrayList<>();
    public Deck() {
        for (Card.Suit s : EnumSet.range(Card.Suit.SPADE, Card.Suit.CLUB)) {
            for (int r = 1; r <= 13; r++) cards.add(new Card(s, r));
        }
        cards.add(new Card(Card.Suit.JOKER, 0));
        Collections.shuffle(cards);
    }
    public Card draw() { return cards.isEmpty() ? null : cards.remove(0); }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OldMaidGUI().setVisible(true));
    }
} // OldMaidGUIの終わり

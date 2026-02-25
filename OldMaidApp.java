import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

// --- ゲームの基本ルール（Card, Hand, Deck） ---
class Card {
    enum Suit { SPADE, HEART, DIAMOND, CLUB, JOKER }
    private final Suit suit;
    private final int rank;
    public Card(Suit suit, int rank) { this.suit = suit; this.rank = rank; }
    public Suit getSuit() { return suit; }
    public int getRank() { return rank; }
    public boolean isJoker() { return suit == Suit.JOKER; }
    public Color getColor() { return (suit == Suit.HEART || suit == Suit.DIAMOND) ? Color.RED : Color.BLACK; }
}

class Hand {
    private List<Card> cards = new ArrayList<>();
    public void addCard(Card c) { cards.add(c); }
    public int size() { return cards.size(); }
    public List<Card> getCards() { return cards; }
    public Card pullCard(int idx) { return cards.remove(idx); }
    public void discardPairs() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : cards) if (!c.isJoker()) counts.put(c.getRank(), counts.getOrDefault(c.getRank(), 0) + 1);
        for (Integer rank : counts.keySet()) {
            int pairs = counts.get(rank) / 2;
            for (int i = 0; i < pairs * 2; i++) {
                for (int j = 0; j < cards.size(); j++) {
                    if (!cards.get(j).isJoker() && cards.get(j).getRank() == rank) { cards.remove(j); break; }
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

// --- メイン画面（GUI） ---
public class OldMaidApp extends JFrame {
    private List<Hand> allHands;
    private JLabel statusLabel;
    private Socket socket;
    private PrintWriter out;

    public OldMaidApp() {
        setTitle("Old Maid Online");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        statusLabel = new JLabel("サーバーに接続中...", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.CENTER);
        
        connectToServer();
        initGame();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                Scanner in = new Scanner(socket.getInputStream());
                while (in.hasNextLine()) {
                    String msg = in.nextLine();
                    SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("オフラインモード"));
            }
        }).start();
    }

    private void initGame() {
        allHands = new ArrayList<>();
        allHands.add(new Hand()); // 自分用
        allHands.add(new Hand()); // CPU用
        Deck deck = new Deck();
        int turn = 0;
        while (true) {
            Card c = deck.draw();
            if (c == null) break;
            allHands.get(turn % 2).addCard(c);
            turn++;
        }
        for (Hand h : allHands) h.discardPairs();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OldMaidApp().setVisible(true));
    }
}

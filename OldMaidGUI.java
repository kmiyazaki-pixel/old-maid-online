package com.game.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import com.game.model.*;

public class OldMaidGUI extends JFrame {
    private List<Hand> allHands;
    private List<CardPanel> cpuPanels;
    private CardPanel playerPanel;
    private JLabel statusLabel;
    private int playerCount = 2;
    private int currentPlayerIdx = 0; 
    private boolean isPlayerTurn = false;

    // --- 追加：ネットワーク通信用 ---
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public OldMaidGUI() {
        setTitle("Supreme Old Maid - Online Edition");
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
            }
        };
        setContentPane(contentPane);

        connectToServer(); // 起動時にサーバーへ接続を試みる
        selectPlayerCount();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "サーバーが見つかりません。先にServerを起動してください。"));
            }
        }).start();
    }

    private void selectPlayerCount() {
        Integer[] options = {2, 3, 4, 5, 6};
        Integer res = (Integer) JOptionPane.showInputDialog(this, "対戦人数を選んでください", "Start", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
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
        statusLabel = new JLabel("サーバー接続待機中...", SwingConstants.CENTER);
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Serif", Font.BOLD, 24));
        playerPanel = new CardPanel(allHands.get(0), false, "YOU");
        add(topArea, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);
        revalidate(); repaint();
    }

    private void nextTurn() {
        if (allHands.get(currentPlayerIdx).size() == 0) { passTurn(); return; }
        if (currentPlayerIdx == 0) {
            isPlayerTurn = true;
            statusLabel.setText("あなたの番です");
        } else {
            isPlayerTurn = false;
            startCpuAi(currentPlayerIdx);
        }
    }

    private void startCpuAi(int idx) {
        int target = (idx + 1) % playerCount;
        while (allHands.get(target).size() == 0) target = (target + 1) % playerCount;
        int finalTarget = target;
        new Timer(1500, e -> executeDraw(idx, finalTarget, (int)(Math.random() * allHands.get(finalTarget).size()))).start();
    }

    private void executeDraw(int drawer, int victim, int cardIdx) {
        Card picked = allHands.get(victim).pullCard(cardIdx);
        allHands.get(drawer).addCard(picked);
        if (out != null) out.println("Player " + drawer + " drew a card from " + victim);
        repaint();
        new Timer(1000, e -> {
            allHands.get(drawer).discardPairs();
            repaint();
            if (!checkGameOver()) passTurn();
        }).start();
    }

    private void passTurn() { currentPlayerIdx = (currentPlayerIdx + 1) % playerCount; nextTurn(); }

    private boolean checkGameOver() {
        int active = 0;
        for (Hand h : allHands) if (h.size() > 0) active++;
        if (active <= 1) {
            JOptionPane.showMessageDialog(this, "ゲーム終了！");
            return true;
        }
        return false;
    }

    class CardPanel extends JPanel {
        private Hand hand;
        private boolean isCPU;
        private String name;
        public CardPanel(Hand hand, boolean isCPU, String name) {
            this.hand = hand; this.isCPU = isCPU

import java.io.*;
import java.net.*;
import java.util.*;

public class OldMaidServer {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static List<String> deck = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(12345);
        System.out.println("ババ抜きサーバー稼働中...");

        // 山札の準備 (S1=スペード1, J0=ジョーカー...)
        String[] suits = {"S", "H", "D", "C"};
        for (String s : suits) {
            for (int i = 1; i <= 13; i++) deck.add(s + i);
        }
        deck.add("J0");
        Collections.shuffle(deck);

        while (true) {
            Socket s = server.accept();
            ClientHandler handler = new ClientHandler(s);
            clients.add(handler);
            new Thread(handler).start();
            
            // 2人揃ったらカードを配る
            if (clients.size() == 2) {
                distributeCards();
            }
        }
    }

    private static void distributeCards() {
        int playerIdx = 0;
        for (String card : deck) {
            clients.get(playerIdx % 2).out.println("CARD:" + card);
            playerIdx++;
        }
        broadcast("GAME_START:対戦を開始します！");
    }

    private static void broadcast(String msg) {
        for (ClientHandler c : clients) c.out.println(msg);
    }

    static class ClientHandler implements Runnable {
        PrintWriter out;
        BufferedReader in;
        public ClientHandler(Socket s) throws IOException {
            out = new PrintWriter(s.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("受信: " + msg);
                }
            } catch (IOException e) { }
        }
    }
}

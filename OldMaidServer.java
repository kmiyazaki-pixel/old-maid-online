import java.io.*;
import java.net.*;
import java.util.*;

public class OldMaidServer {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int turn = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(12345);
        System.out.println("--- ババ抜き中央管理サーバー起動 ---");

        while (true) {
            Socket s = server.accept();
            ClientHandler handler = new ClientHandler(s, clients.size());
            clients.add(handler);
            new Thread(handler).start();
            System.out.println("プレイヤー " + handler.id + " が接続しました。");

            if (clients.size() == 2) {
                startGame();
            }
        }
    }

    private static void startGame() {
        List<String> deck = new ArrayList<>();
        String[] suits = {"S", "H", "D", "C"};
        for (String s : suits) for (int i = 1; i <= 13; i++) deck.add(s + i);
        deck.add("J0");
        Collections.shuffle(deck);

        for (int i = 0; i < deck.size(); i++) {
            clients.get(i % 2).send("CARD_ADD:" + deck.get(i));
        }
        broadcast("INFO:ゲーム開始！ プレイヤー0の番です。");
        clients.get(0).send("YOUR_TURN");
    }

    public static void broadcast(String msg) {
        for (ClientHandler c : clients) c.send(msg);
    }

    static class ClientHandler implements Runnable {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        int id;

        public ClientHandler(Socket s, int id) throws IOException {
            this.socket = s;
            this.id = id;
            this.out = new PrintWriter(s.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }

        public void send(String msg) { out.println(msg); }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("DRAW_REQUEST:")) {
                        // 引く処理の同期
                        int targetId = (id == 0) ? 1 : 0;
                        broadcast("DRAW_ACTION:" + id + ":" + targetId);
                        // 次のターンへ
                        turn = (turn + 1) % 2;
                        clients.get(turn).send("YOUR_TURN");
                    }
                }
            } catch (IOException e) { clients.remove(this); }
        }
    }
}

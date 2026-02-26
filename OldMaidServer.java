import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;

public class OldMaidServer extends WebSocketServer {
    private static List<WebSocket> conns = new ArrayList<>();

    public OldMaidServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conns.add(conn);
        System.out.println("新規接続: " + conn.getRemoteSocketAddress());

        if (conns.size() == 2) {
            // 1. 53枚のカードを準備してシャッフル
            List<Integer> deck = new ArrayList<>();
            for (int i = 1; i <= 53; i++) deck.add(i);
            Collections.shuffle(deck);

            // 2. 2人に分ける
            List<Integer> rawHand1 = new ArrayList<>(deck.subList(0, 27));
            List<Integer> rawHand2 = new ArrayList<>(deck.subList(27, 53));

            // 3. 【新機能】同じ数字のペアを捨てる
            List<Integer> hand1 = removePairs(rawHand1);
            List<Integer> hand2 = removePairs(rawHand2);

            // 4. 結果を送信
            conns.get(0).send("HAND:" + listToString(hand1));
            conns.get(1).send("HAND:" + listToString(hand2));
            broadcast("GAME_START:対戦開始！ペアを捨てました。");
        }
    }

    // --- ペアを捨てる魔法のメソッド ---
    private List<Integer> removePairs(List<Integer> hand) {
        // 数字(1〜13)ごとにカードを分類する箱を作る
        Map<Integer, List<Integer>> groups = new HashMap<>();
        List<Integer> result = new ArrayList<>();

        for (int card : hand) {
            if (card == 53) { // ジョーカーはそのまま残す
                result.add(card);
                continue;
            }
            // カード番号(1〜52)をトランプの数字(1〜13)に変換
            int value = (card - 1) % 13 + 1;
            groups.computeIfAbsent(value, k -> new ArrayList<>()).add(card);
        }

        // 各数字について、枚数が奇数なら1枚残し、偶数なら全部捨てる
        for (List<Integer> cards : groups.values()) {
            if (cards.size() % 2 != 0) {
                result.add(cards.get(0)); // 1枚だけ残す
            }
        }
        return result;
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        broadcast(message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }

    @Override
    public void onStart() { System.out.println("Server Started"); }

    public void broadcast(String msg) {
        for (WebSocket sock : conns) sock.send(msg);
    }

    private String listToString(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;
        OldMaidServer server = new OldMaidServer(port);
        server.start();
    }
}

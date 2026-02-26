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
    @Override
public void onOpen(WebSocket conn, ClientHandshake handshake) {
    conns.add(conn);
    System.out.println("新規接続: " + conn.getRemoteSocketAddress());

    if (conns.size() == 2) {
        // 1. 53枚のカード（1〜53）を準備
        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= 53; i++) deck.add(i);

        // 2. シャッフル（混ぜる）
        Collections.shuffle(deck);

        // 3. 2人に分ける
        List<Integer> hand1 = new ArrayList<>(deck.subList(0, 27)); // 27枚
        List<Integer> hand2 = new ArrayList<>(deck.subList(27, 53)); // 26枚

        // 4. それぞれに「あなたの手札」として送る
        // フォーマット: HAND:1,5,12,44...
        conns.get(0).send("HAND:" + listToString(hand1));
        conns.get(1).send("HAND:" + listToString(hand2));
        
        broadcast("GAME_START:対戦開始！");
    }
}

// リストをカンマ区切りの文字列にするための便利機能
private String listToString(List<Integer> list) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.size(); i++) {
        sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
    }
    return sb.toString();
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

    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;
        OldMaidServer server = new OldMaidServer(port);
        server.start();
        System.out.println("Port: " + port);
    }
}

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
            // 53枚のデッキを作成してシャッフル
            List<Integer> deck = new ArrayList<>();
            for (int i = 1; i <= 53; i++) deck.add(i);
            Collections.shuffle(deck);

            // 2人に配る（演出のため、ペアを捨てる前の状態を送る）
            List<Integer> hand1 = new ArrayList<>(deck.subList(0, 27));
            List<Integer> hand2 = new ArrayList<>(deck.subList(27, 53));

            conns.get(0).send("HAND:" + listToString(hand1));
            conns.get(1).send("HAND:" + listToString(hand2));
            
            broadcast("GAME_START:対戦開始！");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // チャットや着手情報を全員に転送
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

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
            broadcast("GAME_START:対戦開始！");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // クライアントからの「カード引いた」などのメッセージを全員に転送
        broadcast(message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }

    @Override
    public void onStart() { System.out.println("Web対応サーバー起動完了"); }

    public void broadcast(String msg) {
        for (WebSocket sock : conns) sock.send(msg);
    }
public static void main(String[] args) {
        // Renderが割り当てるポート番号を最優先で読み込む
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;

        // 0.0.0.0 で待機しないと外部（ブラウザ）から繋がらないことがあります
        OldMaidServer server = new OldMaidServer(port);
        server.start();
        System.out.println("Web対応サーバーがポート " + port + " で起動しました。");
    }
    
}

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;

public class OldMaidServer extends WebSocketServer {
    private static List<WebSocket> conns = new ArrayList<>();
    private static Map<WebSocket, List<Integer>> hands = new HashMap<>();
    private int turnIndex = 0; 

    public OldMaidServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (conns.size() >= 2) {
            conn.close();
            return;
        }
        conns.add(conn);
        System.out.println("New Player: " + conn.getRemoteSocketAddress());

        if (conns.size() == 2) {
            List<Integer> deck = new ArrayList<>();
            for (int i = 1; i <= 53; i++) deck.add(i);
            Collections.shuffle(deck);

            hands.put(conns.get(0), new ArrayList<>(deck.subList(0, 27)));
            hands.put(conns.get(1), new ArrayList<>(deck.subList(27, 53)));

            sendState();
        }
    }

    private void sendState() {
        for (int i = 0; i < conns.size(); i++) {
            WebSocket ws = conns.get(i);
            WebSocket opponent = conns.get((i + 1) % 2);
            
            String myHand = listToString(hands.get(ws));
            int opponentCount = hands.get(opponent).size();
            boolean isMyTurn = (i == turnIndex);

            ws.send("STATE|" + myHand + "|" + opponentCount + "|" + isMyTurn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("DRAW:")) {
            int drawIndex = Integer.parseInt(message.replace("DRAW:", ""));
            WebSocket drawer = conns.get(turnIndex);
            WebSocket owner = conns.get((turnIndex + 1) % 2);

            if (conn == drawer && drawIndex < hands.get(owner).size()) {
                int pickedCard = hands.get(owner).remove(drawIndex);
                hands.get(drawer).add(pickedCard);
                turnIndex = (turnIndex + 1) % 2; // ターン交代
                sendState();
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);
        hands.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override
    public void onStart() { System.out.println("Server Started"); }

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
        new OldMaidServer(port).start();
    }
}

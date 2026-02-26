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
        if (conns.size() >= 2) { conn.close(); return; }
        conns.add(conn);
        if (conns.size() == 2) {
            List<Integer> deck = new ArrayList<>();
            for (int i = 1; i <= 53; i++) deck.add(i);
            Collections.shuffle(deck);
            hands.put(conns.get(0), new ArrayList<>(deck.subList(0, 27)));
            hands.put(conns.get(1), new ArrayList<>(deck.subList(27, 53)));
            sendState();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("DRAW:")) {
            int drawIndex = Integer.parseInt(message.replace("DRAW:", ""));
            WebSocket drawer = conns.get(turnIndex);
            WebSocket owner = conns.get((turnIndex + 1) % 2);

            if (conn == drawer && drawIndex < hands.get(owner).size()) {
                // カードを移動
                int pickedCard = hands.get(owner).remove(drawIndex);
                hands.get(drawer).add(pickedCard);
                
                // ここではまだターンを交代せず、ペアを捨てる演出のために現在の状態を送る
                sendState();
            }
        } else if (message.equals("DISCARD_DONE")) {
            // 捨て終わった報告を受けたら、サーバーでカードを消してターンを回す
            hands.put(conn, removePairs(hands.get(conn)));
            
            // 全員の手札をチェックして、全員が捨て終わった状態ならターンを交代
            turnIndex = (turnIndex + 1) % 2;
            checkWinner();
            sendState();
        }
    }

    private List<Integer> removePairs(List<Integer> hand) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        List<Integer> result = new ArrayList<>();
        for (int card : hand) {
            if (card == 53) { result.add(card); continue; }
            int value = (card - 1) % 13 + 1;
            groups.computeIfAbsent(value, k -> new ArrayList<>()).add(card);
        }
        for (List<Integer> cards : groups.values()) {
            if (cards.size() % 2 != 0) { result.add(cards.get(0)); }
        }
        return result;
    }

    private void checkWinner() {
        for (WebSocket ws : conns) {
            if (hands.get(ws).isEmpty()) {
                ws.send("WINNER:YOU WIN!");
                conns.get((conns.indexOf(ws) + 1) % 2).send("WINNER:YOU LOSE...");
            }
        }
    }

    private void sendState() {
        for (int i = 0; i < conns.size(); i++) {
            WebSocket ws = conns.get(i);
            String myHand = listToString(hands.get(ws));
            int oppCount = hands.get(conns.get((i + 1) % 2)).size();
            ws.send("STATE|" + myHand + "|" + oppCount + "|" + (i == turnIndex));
        }
    }

    private String listToString(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
        }
        return sb.toString();
    }

    @Override public void onClose(WebSocket c, int o, String r, boolean m) { conns.remove(c); hands.remove(c); }
    @Override public void onError(WebSocket c, Exception e) { e.printStackTrace(); }
    @Override public void onStart() { System.out.println("Server Started"); }
    public static void main(String[] args) {
        String p = System.getenv("PORT");
        new OldMaidServer(p != null ? Integer.parseInt(p) : 8080).start();
    }
}

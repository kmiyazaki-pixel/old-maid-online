import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OldMaidServer extends WebSocketServer {
    private static Map<String, List<WebSocket>> rooms = new ConcurrentHashMap<>();
    private static Map<WebSocket, List<Integer>> hands = new ConcurrentHashMap<>();
    private static Map<WebSocket, String> connRoom = new ConcurrentHashMap<>();
    private static Map<String, Integer> roomTurns = new ConcurrentHashMap<>();
    private static Map<String, Set<WebSocket>> rematchRequests = new ConcurrentHashMap<>();

    public OldMaidServer(int port) { super(new InetSocketAddress(port)); }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {}

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("JOIN_ROOM:")) {
            joinRoom(conn, message.replace("JOIN_ROOM:", "").trim());
        } else if (message.equals("REMATCH_REQUEST")) {
            handleRematch(conn);
        } else if (message.startsWith("DRAW:")) {
            handleDraw(conn, Integer.parseInt(message.replace("DRAW:", "")));
        } else if (message.equals("DISCARD_DONE")) {
            handleDiscardDone(conn);
        }
    }

    private void joinRoom(WebSocket conn, String roomId) {
        rooms.putIfAbsent(roomId, new ArrayList<>());
        List<WebSocket> members = rooms.get(roomId);
        if (members.size() >= 2) return;
        members.add(conn);
        connRoom.put(conn, roomId);
        if (members.size() == 2) initGame(roomId);
        else conn.send("WAITING:相手を待っています...");
    }

    private void initGame(String roomId) {
        List<WebSocket> m = rooms.get(roomId);
        roomTurns.put(roomId, 0);
        rematchRequests.remove(roomId);
        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= 53; i++) deck.add(i);
        Collections.shuffle(deck);
        // 最初の手札配分
        hands.put(m.get(0), removePairs(new ArrayList<>(deck.subList(0, 27))));
        hands.put(m.get(1), removePairs(new ArrayList<>(deck.subList(27, 53))));
        sendState(roomId);
    }

    private void handleDraw(WebSocket conn, int index) {
        String rid = connRoom.get(conn);
        List<WebSocket> m = rooms.get(rid);
        int turn = roomTurns.get(rid);
        if (conn != m.get(turn)) return;

        WebSocket owner = m.get((turn + 1) % 2);
        if (index >= hands.get(owner).size()) return;

        int card = hands.get(owner).remove(index);
        hands.get(conn).add(card);
        // カードを引いた直後の状態を送信（クライアントでペア捨て演出開始）
        sendState(rid);
    }

    private void handleDiscardDone(WebSocket conn) {
        String rid = connRoom.get(conn);
        // ペアを実際に削除
        hands.put(conn, removePairs(hands.get(conn)));
        // ターンを交代
        roomTurns.put(rid, (roomTurns.get(rid) + 1) % 2);
        
        if (checkWinner(rid)) return;
        sendState(rid);
    }

    private boolean checkWinner(String rid) {
        List<WebSocket> m = rooms.get(rid);
        WebSocket p1 = m.get(0);
        WebSocket p2 = m.get(1);
        boolean p1Empty = hands.get(p1).isEmpty();
        boolean p2Empty = hands.get(p2).isEmpty();

        if (p1Empty || p2Empty) {
            if (p1Empty && p2Empty) { // 同時上がり（理論上稀だが対応）
                p1.send("WINNER:DRAW"); p2.send("WINNER:DRAW");
            } else if (p1Empty) {
                p1.send("WINNER:YOU WIN!"); p2.send("WINNER:YOU LOSE...");
            } else {
                p2.send("WINNER:YOU WIN!"); p1.send("WINNER:YOU LOSE...");
            }
            return true;
        }
        return false;
    }

    private List<Integer> removePairs(List<Integer> hand) {
        Map<Integer, List<Integer>> g = new HashMap<>();
        List<Integer> r = new ArrayList<>();
        for (int c : hand) {
            if (c == 53) { r.add(c); continue; }
            int v = (c - 1) % 13 + 1;
            g.computeIfAbsent(v, k -> new ArrayList<>()).add(c);
        }
        for (List<Integer> cs : g.values()) { if (cs.size() % 2 != 0) r.add(cs.get(0)); }
        return r;
    }

    private void handleRematch(WebSocket conn) {
        String rid = connRoom.get(conn);
        rematchRequests.putIfAbsent(rid, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        rematchRequests.get(rid).add(conn);
        if (rematchRequests.get(rid).size() == 2) initGame(rid);
        else broadcastToRoom(rid, "REMATCH_STATUS:1/2");
    }

    private void sendState(String rid) {
        List<WebSocket> m = rooms.get(rid);
        int t = roomTurns.get(rid);
        for (int i = 0; i < 2; i++) {
            String h = listToString(hands.get(m.get(i)));
            int oc = hands.get(m.get((i + 1) % 2)).size();
            m.get(i).send("STATE|" + h + "|" + oc + "|" + (i == t));
        }
    }

    private void broadcastToRoom(String rid, String msg) {
        for (WebSocket ws : rooms.get(rid)) ws.send(msg);
    }

    private String listToString(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
        return sb.toString();
    }

    @Override public void onClose(WebSocket c, int o, String r, boolean m) {
        String rid = connRoom.get(c);
        if (rid != null && rooms.get(rid) != null) {
            broadcastToRoom(rid, "OPPONENT_LEFT");
            rooms.get(rid).remove(c);
        }
    }
    @Override public void onError(WebSocket c, Exception e) {}
    @Override public void onStart() {}
    public static void main(String[] args) { new OldMaidServer(8080).start(); }
}

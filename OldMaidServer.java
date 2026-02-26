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
    // 再戦希望者を管理 (ルームID -> セット)
    private static Map<String, Set<WebSocket>> rematchRequests = new ConcurrentHashMap<>();

    public OldMaidServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New Connection");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("JOIN_ROOM:")) {
            String roomId = message.replace("JOIN_ROOM:", "").trim();
            joinRoom(conn, roomId);
        } else if (message.equals("REMATCH_REQUEST")) {
            handleRematch(conn);
        } else if (message.equals("LEAVE_ROOM")) {
            leaveRoom(conn);
        } else {
            handleGameMessage(conn, message);
        }
    }

    private void joinRoom(WebSocket conn, String roomId) {
        rooms.putIfAbsent(roomId, new ArrayList<>());
        List<WebSocket> members = rooms.get(roomId);
        if (members.size() >= 2) {
            conn.send("ERROR:満員です");
            return;
        }
        members.add(conn);
        connRoom.put(conn, roomId);
        if (members.size() == 2) {
            initGame(roomId);
        } else {
            conn.send("WAITING:相手を待っています...");
        }
    }

    private void initGame(String roomId) {
        List<WebSocket> members = rooms.get(roomId);
        roomTurns.put(roomId, 0);
        rematchRequests.remove(roomId); // 再戦状態をリセット
        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= 53; i++) deck.add(i);
        Collections.shuffle(deck);
        hands.put(members.get(0), new ArrayList<>(deck.subList(0, 27)));
        hands.put(members.get(1), new ArrayList<>(deck.subList(27, 53)));
        sendState(roomId);
    }

    private void handleRematch(WebSocket conn) {
        String roomId = connRoom.get(conn);
        if (roomId == null) return;
        rematchRequests.putIfAbsent(roomId, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        rematchRequests.get(roomId).add(conn);
        
        broadcastToRoom(roomId, "REMATCH_STATUS:" + rematchRequests.get(roomId).size());

        if (rematchRequests.get(roomId).size() == 2) {
            initGame(roomId);
        }
    }

    private void handleGameMessage(WebSocket conn, String message) {
        String roomId = connRoom.get(conn);
        if (roomId == null) return;
        List<WebSocket> members = rooms.get(roomId);
        if (members.size() < 2) return;
        int turnIndex = roomTurns.get(roomId);

        if (message.startsWith("DRAW:")) {
            int drawIndex = Integer.parseInt(message.replace("DRAW:", ""));
            if (conn == members.get(turnIndex)) {
                int picked = hands.get(members.get((turnIndex + 1) % 2)).remove(drawIndex);
                hands.get(conn).add(picked);
                sendState(roomId);
            }
        } else if (message.equals("DISCARD_DONE")) {
            hands.put(conn, removePairs(hands.get(conn)));
            roomTurns.put(roomId, (turnIndex + 1) % 2);
            checkWinner(roomId);
            sendState(roomId);
        }
    }

    private void leaveRoom(WebSocket conn) {
        String roomId = connRoom.get(conn);
        if (roomId != null) {
            broadcastToRoom(roomId, "OPPONENT_LEFT");
            onClose(conn, 0, "", false);
        }
    }

    private List<Integer> removePairs(List<Integer> hand) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        List<Integer> result = new ArrayList<>();
        for (int c : hand) {
            if (c == 53) { result.add(c); continue; }
            int v = (c - 1) % 13 + 1;
            groups.computeIfAbsent(v, k -> new ArrayList<>()).add(c);
        }
        for (List<Integer> cs : groups.values()) { if (cs.size() % 2 != 0) result.add(cs.get(0)); }
        return result;
    }

    private void checkWinner(String roomId) {
        for (WebSocket ws : rooms.get(roomId)) {
            if (hands.get(ws).isEmpty()) {
                ws.send("WINNER:YOU WIN!");
                rooms.get(roomId).get((rooms.get(roomId).indexOf(ws) + 1) % 2).send("WINNER:YOU LOSE...");
            }
        }
    }

    private void sendState(String roomId) {
        List<WebSocket> m = rooms.get(roomId);
        int t = roomTurns.get(roomId);
        for (int i = 0; i < m.size(); i++) {
            String h = listToString(hands.get(m.get(i)));
            int oc = hands.get(m.get((i + 1) % 2)).size();
            m.get(i).send("STATE|" + h + "|" + oc + "|" + (i == t));
        }
    }

    private void broadcastToRoom(String roomId, String msg) {
        if (rooms.containsKey(roomId)) {
            for (WebSocket ws : rooms.get(roomId)) ws.send(msg);
        }
    }

    private String listToString(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
        return sb.toString();
    }

    @Override public void onClose(WebSocket c, int o, String r, boolean m) {
        String rid = connRoom.get(c);
        if (rid != null && rooms.get(rid) != null) {
            rooms.get(rid).remove(c);
            if (rooms.get(rid).isEmpty()) rooms.remove(rid);
        }
        hands.remove(c);
        connRoom.remove(c);
    }

    @Override public void onError(WebSocket c, Exception e) { e.printStackTrace(); }
    @Override public void onStart() { System.out.println("Server Started"); }
    public static void main(String[] args) {
        String p = System.getenv("PORT");
        new OldMaidServer(p != null ? Integer.parseInt(p) : 8080).start();
    }
}

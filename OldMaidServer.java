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

    public OldMaidServer(int port) { super(new InetSocketAddress(port)); }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New Connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (message.startsWith("JOIN_ROOM:")) {
                handleJoinRoom(conn, message.replace("JOIN_ROOM:", "").trim());
            } else if (message.startsWith("DRAW:")) {
                handleDraw(conn, Integer.parseInt(message.replace("DRAW:", "")));
            } else if (message.equals("DISCARD_DONE")) {
                handleDiscardDone(conn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleJoinRoom(WebSocket conn, String roomId) {
        leaveCurrentRoom(conn);
        rooms.putIfAbsent(roomId, new ArrayList<>());
        List<WebSocket> members = rooms.get(roomId);

        if (members.size() >= 2) {
            conn.send("ERROR:Room is Full");
            return;
        }

        members.add(conn);
        connRoom.put(conn, roomId);
        
        if (members.size() == 2) {
            startGame(roomId);
        } else {
            conn.send("WAITING:Waiting for opponent...");
        }
    }

    private void startGame(String roomId) {
        List<WebSocket> m = rooms.get(roomId);
        if (m.size() < 2) return;

        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= 53; i++) deck.add(i);
        Collections.shuffle(deck);

        hands.put(m.get(0), new ArrayList<>(deck.subList(0, 27)));
        hands.put(m.get(1), new ArrayList<>(deck.subList(27, 53)));
        
        // 開始時もシャッフル
        Collections.shuffle(hands.get(m.get(0)));
        Collections.shuffle(hands.get(m.get(1)));
        
        roomTurns.put(roomId, 0);
        sendGameState(roomId);
    }

    private void handleDraw(WebSocket conn, int index) {
        String rid = connRoom.get(conn);
        if (rid == null) return;
        List<WebSocket> m = rooms.get(rid);
        int turn = roomTurns.get(rid);

        if (conn != m.get(turn)) return;

        WebSocket target = m.get((turn + 1) % 2);
        List<Integer> targetHand = hands.get(target);

        if (index < 0 || index >= targetHand.size()) {
            index = 0;
        }

        int pulledCard = targetHand.remove(index);
        hands.get(conn).add(pulledCard);

        // シャッフルして位置をわからなくする
        Collections.shuffle(hands.get(conn));
        Collections.shuffle(targetHand);

        sendGameState(rid);
    }

    private void handleDiscardDone(WebSocket conn) {
        String rid = connRoom.get(conn);
        if (rid == null) return;

        hands.put(conn, removePairs(hands.get(conn)));
        
        // ペアを捨てた後もシャッフル
        Collections.shuffle(hands.get(conn));
        
        roomTurns.put(rid, (roomTurns.get(rid) + 1) % 2);

        if (!checkWinner(rid)) {
            sendGameState(rid);
        }
    }

    private boolean checkWinner(String rid) {
        List<WebSocket> m = rooms.get(rid);
        WebSocket p1 = m.get(0), p2 = m.get(1);
        int s1 = hands.get(p1).size(), s2 = hands.get(p2).size();

        if (s1 == 0 || s2 == 0) {
            if (s1 == 0 && s2 == 0) {
                p1.send("WINNER:DRAW"); p2.send("WINNER:DRAW");
            } else if (s1 == 0) {
                p1.send("WINNER:YOU WIN!"); p2.send("WINNER:YOU LOSE...");
            } else {
                p2.send("WINNER:YOU WIN!"); p1.send("WINNER:YOU LOSE...");
            }
            return true;
        }
        return false;
    }

    private void sendGameState(String rid) {
        List<WebSocket> m = rooms.get(rid);
        int turnIdx = roomTurns.get(rid);

        for (int i = 0; i < 2; i++) {
            WebSocket player = m.get(i);
            String myHandStr = String.join(",", hands.get(player).stream().map(String::valueOf).toArray(String[]::new));
            int oppCardCount = hands.get(m.get((i + 1) % 2)).size();
            boolean isMyTurn = (i == turnIdx);
            player.send("STATE|" + myHandStr + "|" + oppCardCount + "|" + isMyTurn);
        }
    }

    private List<Integer> removePairs(List<Integer> hand) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int c : hand) {
            if (c == 53) { counts.put(53, 1); continue; }
            int v = (c - 1) % 13 + 1;
            counts.put(v, counts.getOrDefault(v, 0) + 1);
        }
        List<Integer> result = new ArrayList<>();
        for (int c : hand) {
            if (c == 53) { result.add(53); continue; }
            int v = (c - 1) % 13 + 1;
            if (counts.get(v) % 2 != 0) {
                result.add(c);
                counts.put(v, 0);
            }
        }
        return result;
    }

    private void leaveCurrentRoom(WebSocket conn) {
        String rid = connRoom.get(conn);
        if (rid != null && rooms.containsKey(rid)) {
            rooms.get(rid).remove(conn);
            if (rooms.get(rid).isEmpty()) rooms.remove(rid);
        }
        connRoom.remove(conn);
    }

    @Override public void onClose(WebSocket c, int code, String reason, boolean remote) { leaveCurrentRoom(c); }
    @Override public void onError(WebSocket c, Exception e) { e.printStackTrace(); }
    @Override public void onStart() { System.out.println("Server Started!"); }

    public static void main(String[] args) {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null) port = Integer.parseInt(envPort);
        new OldMaidServer(port).start();
    }
}

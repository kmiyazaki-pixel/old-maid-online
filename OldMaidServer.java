import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OldMaidServer extends WebSocketServer {
    // ルームIDごとの接続リスト
    private static Map<String, List<WebSocket>> rooms = new ConcurrentHashMap<>();
    // 接続ごとの手札データ
    private static Map<WebSocket, List<Integer>> hands = new ConcurrentHashMap<>();
    // 接続ごとの所属ルームID
    private static Map<WebSocket, String> connRoom = new ConcurrentHashMap<>();
    // ルームごとのターン管理 (0 or 1)
    private static Map<String, Integer> roomTurns = new ConcurrentHashMap<>();

    public OldMaidServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New Connection. Waiting for Room ID...");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // 1. ルーム参加処理
        if (message.startsWith("JOIN_ROOM:")) {
            String roomId = message.replace("JOIN_ROOM:", "").trim();
            joinRoom(conn, roomId);
            return;
        }

        String roomId = connRoom.get(conn);
        if (roomId == null) return;

        List<WebSocket> members = rooms.get(roomId);
        if (members == null || members.size() < 2) return;

        int turnIndex = roomTurns.get(roomId);

        // 2. カードを引く処理
        if (message.startsWith("DRAW:")) {
            int drawIndex = Integer.parseInt(message.replace("DRAW:", ""));
            WebSocket drawer = members.get(turnIndex);
            WebSocket owner = members.get((turnIndex + 1) % 2);

            if (conn == drawer && drawIndex < hands.get(owner).size()) {
                int pickedCard = hands.get(owner).remove(drawIndex);
                hands.get(drawer).add(pickedCard);
                sendState(roomId);
            }
        } 
        // 3. ペア削除完了報告
        else if (message.equals("DISCARD_DONE")) {
            hands.put(conn, removePairs(hands.get(conn)));
            
            // ターン交代
            roomTurns.put(roomId, (turnIndex + 1) % 2);
            checkWinner(roomId);
            sendState(roomId);
        }
    }

    private void joinRoom(WebSocket conn, String roomId) {
        rooms.putIfAbsent(roomId, new ArrayList<>());
        List<WebSocket> members = rooms.get(roomId);

        if (members.size() >= 2) {
            conn.send("ERROR:Room is full");
            return;
        }

        members.add(conn);
        connRoom.put(conn, roomId);

        if (members.size() == 2) {
            // ゲーム初期化
            roomTurns.put(roomId, 0);
            List<Integer> deck = new ArrayList<>();
            for (int i = 1; i <= 53; i++) deck.add(i);
            Collections.shuffle(deck);

            hands.put(members.get(0), new ArrayList<>(deck.subList(0, 27)));
            hands.put(members.get(1), new ArrayList<>(deck.subList(27, 53)));

            sendState(roomId);
        } else {
            conn.send("WAITING:他のプレイヤーを待っています...");
        }
    }

    private void sendState(String roomId) {
        List<WebSocket> members = rooms.get(roomId);
        int turnIndex = roomTurns.get(roomId);

        for (int i = 0; i < members.size(); i++) {
            WebSocket ws = members.get(i);
            String myHand = listToString(hands.get(ws));
            int oppCount = hands.get(members.get((i + 1) % 2)).size();
            ws.send("STATE|" + myHand + "|" + oppCount + "|" + (i == turnIndex));
        }
    }

    private void checkWinner(String roomId) {
        List<WebSocket> members = rooms.get(roomId);
        for (WebSocket ws : members) {
            if (hands.get(ws).isEmpty()) {
                ws.send("WINNER:YOU WIN!");
                members.get((members.indexOf(ws) + 1) % 2).send("WINNER:YOU LOSE...");
            }
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

    private String listToString(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(i == list.size() - 1 ? "" : ",");
        }
        return sb.toString();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String roomId = connRoom.get(conn);
        if (roomId != null) {
            List<WebSocket> members = rooms.get(roomId);
            if (members != null) members.remove(conn);
            if (members == null || members.isEmpty()) rooms.remove(roomId);
        }
        hands.remove(conn);
        connRoom.remove(conn);
    }

    @Override public void onError(WebSocket c, Exception e) { e.printStackTrace(); }
    @Override public void onStart() { System.out.println("Server Started with Room Support"); }

    public static void main(String[] args) {
        String p = System.getenv("PORT");
        new OldMaidServer(p != null ? Integer.parseInt(p) : 8080).start();
    }
}

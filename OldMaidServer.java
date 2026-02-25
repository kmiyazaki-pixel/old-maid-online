import java.io.*;
import java.net.*;
import java.util.*;

public class OldMaidServer {
    private static List<PrintWriter> clientWriters = new ArrayList<>();

    public static void main(String[] args) {
        int port = 12345; // 接続用ポート番号
        System.out.println("ババ抜きサーバーがポート " + port + " で起動しました...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("新しいプレイヤーが接続しました: " + socket.getInetAddress());
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                clientWriters.add(out);
                
                // 接続したプレイヤーにメッセージを送る
                out.println("SERVER: ババ抜き会場へようこそ！現在の参加者: " + clientWriters.size() + "人");
            }
        } catch (IOException e) {
            System.err.println("サーバーエラー: " + e.getMessage());
        }
    }
}

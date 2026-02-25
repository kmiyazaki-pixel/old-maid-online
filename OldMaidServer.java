import java.io.*;
import java.net.*;
import java.util.*;

public class OldMaidServer {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(12345);
        System.out.println("Server started on port 12345...");
        List<PrintWriter> clients = new ArrayList<>();

        while (true) {
            Socket s = server.accept();
            PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
            clients.add(pw);
            System.out.println("New player joined!");
            for (PrintWriter p : clients) p.println("現在の参加人数: " + clients.size());
        }
    }
}

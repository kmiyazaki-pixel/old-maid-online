package com.game.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class OldMaidServer {
    private static List<PrintWriter> clientWriters = new ArrayList<>();

    public static void main(String[] args) {
        int port = 12345;
        System.out.println("--- ババ抜きオンライン サーバー起動中 ---");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("プレイヤーが入室しました: " + socket.getInetAddress());
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                clientWriters.add(out);
                
                // 全員に現在の人数を知らせる
                broadcast("現在の参加人数: " + clientWriters.size() + "人");
            }
        } catch (IOException e) {
            System.err.println("サーバーエラー: " + e.getMessage());
        }
    }

    private static void broadcast(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }
}

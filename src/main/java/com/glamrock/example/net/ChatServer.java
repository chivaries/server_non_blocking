package com.glamrock.example.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

public class ChatServer {
    public void startServer() {
        HashMap<String, Object> hm = new HashMap<>();
        try {
            ServerSocket server = new ServerSocket(10001);
            System.out.println(String.format("[%s] ServerSocket 생성", Thread.currentThread().getName()));

            while(true) {
                Socket sock = server.accept();
                System.out.println(String.format("[%s] 접속 대기", Thread.currentThread().getName()));

                ChatThread chatThread = new ChatThread(sock, hm);
                chatThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ChatThread extends Thread {
        private Socket sock;
        private String id;
        private BufferedReader br;
        private HashMap<String, Object> hm;

        public ChatThread(Socket sock, HashMap<String, Object> hm) {
            this.sock = sock;
            this.hm = hm;

            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                br = new BufferedReader(new InputStreamReader((sock.getInputStream())));
                id = br.readLine();
                
                broadcast(String.format("%s 님이 접속하셨습니다.", id));
                System.out.println(String.format("[%s] %s 님 접속", Thread.currentThread().getName(), id));
                
                synchronized (hm) {
                    hm.put(this.id, pw);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String line;

            try {
                while ((line = br.readLine()) != null) {
                    if (line.equals("/quit")) {
                        break;
                    } else {
                        System.out.println((String.format("[%s] %s 님 메세지 수신 : %s", Thread.currentThread().getName(), id, line)));
                        broadcast(String.format("%s : %s", id, line));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (hm) {
                    hm.remove(id);
                }
                broadcast(String.format("%s 님이 접속 종료했습니다.", id));
                System.out.println((String.format("[%s] %s 님 접속 종료.", Thread.currentThread().getName(), id)));
                try {
                    if(sock != null) {
                        sock.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        private void broadcast(final String msg) {
            synchronized (hm) {
                hm.values()
                        .stream()
                        .map(pw -> (PrintWriter)pw)
                        .forEach(pw -> {
                            pw.println(msg);
                            pw.flush();
                        });
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.startServer();
    }
}

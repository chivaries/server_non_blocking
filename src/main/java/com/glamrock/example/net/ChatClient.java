package com.glamrock.example.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public void startClient(int id) {
        Socket sock = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean endflag = false;
        try {
            sock = new Socket("localhost", 10001);
            System.out.println(String.format("[%s] %s 서버 접속", Thread.currentThread().getName(), id));

            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

            pw.println(id);
            pw.flush();

            InputThread inputThread = new InputThread(sock,br);
            inputThread.start();

            String line = null;

            while((line = keyboard.readLine()) != null) {
                System.out.println(String.format("[%s] %s 입력 대기 ", Thread.currentThread().getName(), id));
                pw.println(line);
                pw.flush();
                if(line.equals("/quit")) {
                    endflag = true;
                    break;
                }
            }

            System.out.println(String.format("[%s] 서버 접속 종료", Thread.currentThread().getName()));
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(pw != null) {
                    pw.close();
                }
                if(br != null) {
                    br.close();
                }
                if(sock != null) {
                    sock.close();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class InputThread extends Thread{
        private Socket sock;
        private BufferedReader br;

        public InputThread(Socket sock, BufferedReader br) {
            this.sock = sock;
            this.br = br;
        }

        @Override
        public void run() {
            try {
                String line;
                while((line = br.readLine()) != null) {
                    System.out.println(String.format("[%s] %s",Thread.currentThread().getName(), line));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    if(br != null) {
                        br.close();
                    }
                    if(sock != null) {
                        sock.close();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.startClient((int)(Math.random() * 100));
    }
}

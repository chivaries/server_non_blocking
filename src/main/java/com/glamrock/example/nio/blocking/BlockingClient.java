package com.glamrock.example.nio.blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class BlockingClient {
    SocketChannel socketChannel;

    public void startClient() {
        Thread thread = new Thread(() -> {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                socketChannel.connect(new InetSocketAddress("localhost", 12345));

                System.out.println(String.format("[%s] SocketChannel 연결 완료 - %s", Thread.currentThread().getName(), socketChannel.toString()));
            } catch (Exception e) {
                System.out.println(String.format("[%s] 서버 통신 실패", Thread.currentThread().getName()));

                if (socketChannel.isOpen()) {
                    stopClient();
                }
                return;
            }
            receive();
        });
        thread.start();

        startWriter();
    }

    public void stopClient() {
        try {
            System.out.println(String.format("[%s] 연결 끊음 - %s", Thread.currentThread().getName(), socketChannel.toString()));

            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        while (true) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(100);

            try {
                System.out.println(String.format("[%s] 데이터 받기 대기",Thread.currentThread().getName()));
                // 서버로 부터 데이터 읽기 (blocking)
                int byteCount = socketChannel.read(byteBuffer);

                if (byteCount == -1) {
                    throw new IOException();
                }

                byteBuffer.flip();
                Charset charset = Charset.forName("UTF-8");
                String data = charset.decode(byteBuffer).toString();

                System.out.println(String.format("[%s] 데이터 받기 성공 : %s",Thread.currentThread().getName(), data));
            } catch (IOException e) {
                System.out.println(String.format("[%s] 서버 통신 안됨", Thread.currentThread().getName()));
                stopClient();
                break;
            }

        }
    }

    private void send(String data) {
        Thread thread = new Thread(() -> {
            try {
                Charset charset = Charset.forName("UTF-8");
                ByteBuffer byteBuffer = charset.encode(data);
                socketChannel.write(byteBuffer);

                System.out.println(String.format("[%s] 데이터 보내기 완료 : %s",Thread.currentThread().getName(), data));
            } catch (IOException e) {
                System.out.println(String.format("[%s] 서버 통신 안됨", Thread.currentThread().getName()));
                stopClient();
            }

        });

        thread.start();
    }

    public void startWriter() {
        while(true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println(String.format("[%s] 메세지 입력 :", Thread.currentThread().getName()));
            String message = scanner.next();
            send(message);
        }
    }

    public static void main(String[] args) {
        BlockingClient blockingClient = new BlockingClient();
        blockingClient.startClient();
    }
}

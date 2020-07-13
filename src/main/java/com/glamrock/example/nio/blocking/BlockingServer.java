package com.glamrock.example.nio.blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingServer {
    ExecutorService executorService;
    ServerSocketChannel serverSocketChannel;
    private Vector<SocketChannel> rooms = new Vector();
    private CharsetDecoder decoder = null;

    public void startServer() throws Exception {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.bind(new InetSocketAddress(12345));

        Charset charset = Charset.forName("UTF-8");
        decoder = charset.newDecoder();
        accept();
        System.out.println(String.format("[%s] serverSocketChannel 생성 후 메인 쓰레드는 다른 일 시작 ", Thread.currentThread().getName()));
    }

    private void accept() {
        executorService.submit(() -> {
            System.out.println(String.format("[%s] accept() 대기 쓰레드 생성 ", Thread.currentThread().getName()));
            while (true) {
                try {
                    System.out.println(String.format("[%s] accept() 대기 ", Thread.currentThread().getName()));
                    // 이 부분에서 연결이 될때까지 블로킹
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    System.out.println(String.format("[%s] %s 클라이언트 접속 (SocketChannel 생성)", Thread.currentThread().getName(), socketChannel.toString()));
                    read(socketChannel);
                } catch(Exception e) {
                    e.printStackTrace();
                    System.out.println(String.format("[%s] 서버 통신 실패 - %s ", Thread.currentThread().getName(), e.getMessage()));
                    if (serverSocketChannel.isOpen()) {
                        stopServer();
                    }
                    break;
                }
            }
        });
    }

    private void read(SocketChannel socketChannel) {
        executorService.submit(() -> {
            System.out.println(String.format("[%s] %s 읽고 쓰기 쓰레드 생성.", Thread.currentThread().getName(), socketChannel.toString()));
            rooms.add(socketChannel);

            while(true) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(100);

                try {
                    System.out.println(String.format("[%s] %s 데이터 받기 대기", Thread.currentThread().getName(), socketChannel.toString()));
                    int byteCount = socketChannel.read(byteBuffer);

                    if (byteCount == -1) {
                        throw new IOException();
                    }
                } catch (IOException ex) {
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    rooms.remove(socketChannel);
                    break;
                }

                try {
                    broadcast(byteBuffer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }

                byteBuffer.clear();
            }

            System.out.println(String.format("[%s] %s 읽고 쓰기 쓰레드 종료", Thread.currentThread().getName(), socketChannel.toString()));
        });
    }

    private void broadcast(ByteBuffer byteBuffer) throws IOException {
        byteBuffer.flip();
        Iterator iterator = rooms.iterator();

        String data = decoder.decode(byteBuffer).toString();
        System.out.println(String.format("[%s] 메세지 받음 - %s", Thread.currentThread().getName(), data));
        byteBuffer.rewind();

        while (iterator.hasNext()) {
            SocketChannel socketChannel = (SocketChannel) iterator.next();

            if (socketChannel != null) {
                System.out.println(String.format("[%s] %s 클라이언트로 메세지 전송", Thread.currentThread().getName(), socketChannel.toString()));
                socketChannel.write(byteBuffer);
                byteBuffer.rewind();
            }
        }
    }

    private void stopServer() {
        try {
            Iterator iterator = rooms.iterator();

            while (iterator.hasNext()) {
                SocketChannel socketChannel = (SocketChannel) iterator.next();
                socketChannel.close();
                iterator.remove();
            }

            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }

            if (executorService != null && executorService.isShutdown()) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BlockingServer blockingServer = new BlockingServer();
        try {
            blockingServer.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

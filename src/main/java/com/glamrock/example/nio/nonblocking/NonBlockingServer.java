package com.glamrock.example.nio.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class NonBlockingServer {
    Selector selector;
    private List<Client> rooms = new ArrayList<>();

    public void initServer() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(12345));
        // Selector 에 accept 작업 등록
        System.out.println(String.format("[%s] Selector 에 accept 작업 등록 ", Thread.currentThread().getName()));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void startServer() {
        Thread thread = new Thread(() -> {
            System.out.println(String.format("[%s] 연결 완료 ", Thread.currentThread().getName()));

            while (true) {
                try {
                    System.out.println(String.format("[%s] Selector 호출 전 ", Thread.currentThread().getName()));

                    // blocking
                    int keyCount = selector.select();

                    System.out.println(String.format("[%s] Selector 호출 후 - keyCount : %s ", Thread.currentThread().getName(), keyCount));

                    if (keyCount == 0) {
                        continue;
                    }

                    Set<SelectionKey> selectedKeys = selector.selectedKeys(); // 선택된 키셋 얻기

                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();

                        System.out.println(String.format("[%s] Selector 호출 후 작업 가능한 키가 선택됨 , selectedKey : %s ", Thread.currentThread().getName(), selectionKey.channel()));

                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            Client client = (Client)selectionKey.attachment();
                            client.receive(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            Client client = (Client)selectionKey.attachment();
                            client.send(selectionKey);
                        }

                        iterator.remove();
                    }
                } catch (Exception e) {
                    break;
                }
            }
        });

        thread.start();
    }

    private void accept(SelectionKey selectionKey) throws Exception {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();

        // 서버소켓 accept() 메소드로 서버소켓을 생성한다.
        SocketChannel socketChannel = server.accept();

        if (socketChannel == null) return;

        System.out.println(String.format("[%s] %s 클라이언트 접속 (SocketChannel 생성) ", Thread.currentThread().getName(), socketChannel.toString()));

        rooms.add(new Client(socketChannel));
    }

    public static void main(String[] args) {
        NonBlockingServer nonBlockingServer = new NonBlockingServer();
        try {
            nonBlockingServer.initServer();
            nonBlockingServer.startServer();
            System.out.println(String.format("[%s] serverSocketChannel 생성 후 메인 쓰레드는 다른 일 시작 ", Thread.currentThread().getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class Client {
        SocketChannel socketChannel;
        String sendData;

        Client(SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;

            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);

            System.out.println(String.format("[%s] %s Selector 에 read 작업 등록 ", Thread.currentThread().getName(), socketChannel.toString()));
        }

        void receive(SelectionKey selectionKey) {

            try {
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                int byteCount = socketChannel.read(byteBuffer);

                if (byteCount == -1) {
                    throw new IOException();
                }

                byteBuffer.flip();
                Charset charset = Charset.forName("UTF-8");
                String data = charset.decode(byteBuffer).toString();

                System.out.println(String.format("[%s] %s 데이터 읽기 : %s ", Thread.currentThread().getName(), socketChannel.toString(), data));

                rooms.stream()
                        .filter(client -> !client.socketChannel.equals(socketChannel))
                        .forEach(client -> {
                            client.sendData = data;
                            SelectionKey key = client.socketChannel.keyFor(selector);
                            key.interestOps(SelectionKey.OP_WRITE);
                            System.out.println(String.format("[%s] %s Selector 에 write 작업 등록 ", Thread.currentThread().getName(), client.socketChannel.toString()));
                        });

                selector.wakeup();
                System.out.println(String.format("[%s] %s selector.wakeup() 호출 ", Thread.currentThread().getName(), socketChannel.toString()));
            } catch (IOException e) {
                try {
                    rooms.remove(this);
                    System.out.println(String.format("[%s] 클라이언트 통신 안됨 : %s]", Thread.currentThread().getName() , socketChannel.toString()));
                    socketChannel.close();
                } catch (IOException e2) {

                }
            }

        }

        void send(SelectionKey selectionKey) {
            try {
                Charset charset = Charset.forName("UTF-8");
                ByteBuffer byteBuffer = charset.encode(sendData);

                socketChannel.write(byteBuffer);
                System.out.println(String.format("[%s] %s 데이터 쓰기 : %s ", Thread.currentThread().getName(), socketChannel.toString(), sendData));

                selectionKey.interestOps(SelectionKey.OP_READ);
                System.out.println(String.format("[%s] %s Selector 에 read 작업 등록 ", Thread.currentThread().getName(), socketChannel.toString()));

                selector.wakeup();
                System.out.println(String.format("[%s] %s selector.wakeup() 호출 ", Thread.currentThread().getName(), socketChannel.toString()));
            } catch (Exception e) {
                try {
                    rooms.remove(this);
                    System.out.println(String.format("[%s] 클라이언트 통신 안됨 : %s]", Thread.currentThread().getName() , socketChannel.toString()));
                    socketChannel.close();
                } catch (IOException e2) {

                }
            }
        }
    }
}

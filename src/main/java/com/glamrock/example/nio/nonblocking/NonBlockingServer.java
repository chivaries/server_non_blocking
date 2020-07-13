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
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class NonBlockingServer {
    Selector selector;
    private Vector<SocketChannel> rooms = new Vector();
    private CharsetDecoder decoder = null;

    public void initServer() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(12345));
        // Selector 에 accept 작업 등록
        System.out.println(String.format("[%s] Selector 에 accept 작업 등록 ", Thread.currentThread().getName()));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        Charset charset = Charset.forName("UTF-8");
        decoder = charset.newDecoder();
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
                            read(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            /* 쓰기 작업 처리 */
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

        if (socketChannel == null)
            return;

        System.out.println(String.format("[%s] %s 클라이언트 접속 (SocketChannel 생성) ", Thread.currentThread().getName(), socketChannel.toString()));

        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println(String.format("[%s] %s Selector 에 read 작업 등록 ", Thread.currentThread().getName(), socketChannel.toString()));

        rooms.add(socketChannel);
    }

    private void read(SelectionKey selectionKey) {
        // SelectionKey 로부터 소켓채널을 얻는다.
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

        try {
            socketChannel.read(byteBuffer);
            System.out.println(String.format("[%s] %s 데이터 읽기 ", Thread.currentThread().getName(), socketChannel.toString()));
        } catch (IOException ex) {
            try {
                socketChannel.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

            rooms.remove(socketChannel);
            ex.printStackTrace();
        }
        
        try {
            broadcast(byteBuffer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        byteBuffer.clear();
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
//                SelectionKey selectionKey = socketChannel.keyFor(selector);
//                selectionKey.interestOps(SelectionKey.OP_WRITE);
                System.out.println(String.format("[%s] %s 클라이언트로 메세지 전송", Thread.currentThread().getName(), socketChannel.toString()));
                socketChannel.write(byteBuffer);
                byteBuffer.rewind();
            }
        }
    }

//    void send(SelectionKey selectionKey) {
//        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
//
//        try {
//            Charset charset = Charset.forName("UTF-8");
//            ByteBuffer byteBuffer = charset.encode(sendData);
//
//            socketChannel.write(byteBuffer);
//            selectionKey.interestOps(SelectionKey.OP_READ);
//            selector.wakeup();
//        } catch (Exception e) {
//            try {
//                rooms.remove(socketChannel);
//                socketChannel.close();
//            } catch (IOException iox) {
//
//            }
//        }
//    }

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
}

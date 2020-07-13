package com.glamrock.example.nio.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Scanner;

public class NonBlockingClient {
    static Selector selector = null;
    private SocketChannel socketChannel = null;

    public void startClient() throws IOException {
        initClient();
        Receive receive = new Receive();
        new Thread(receive).start();
        startWriter();
    }

    private void startWriter() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        try {
            while(true) {
                Scanner scanner = new Scanner(System.in);
                System.out.println(String.format("[%s] 메세지 입력 :", Thread.currentThread().getName()));
                String message = scanner.next();
                byteBuffer.clear();
                byteBuffer.put(message.getBytes());
                byteBuffer.flip();
                // socketChannel 에 write 한다.
                socketChannel.write(byteBuffer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            clearBuffer(byteBuffer);
        }
    }

    private void initClient() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 12345));
        socketChannel.configureBlocking(false);
        // Selector 에 accept 작업 등록
        System.out.println(String.format("[%s] Selector 에 read 작업 등록 ", Thread.currentThread().getName()));
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    public static void clearBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
    }

    public static void main(String[] args) {
        NonBlockingClient client = new NonBlockingClient();
        try {
            client.startClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Receive implements Runnable {
    private CharsetDecoder decoder = null;

    @Override
    public void run() {
        Charset charset = Charset.forName("UTF-8");
        decoder = charset.newDecoder();
        try {
            while (true) {
                System.out.println(String.format("[%s] Selector 호출 전 ", Thread.currentThread().getName()));
                int keyCount = NonBlockingClient.selector.select();
                System.out.println(String.format("[%s] Selector 호출 후 - keyCount : %s ", Thread.currentThread().getName(), keyCount));

                if (keyCount == 0) {
                    continue;
                }

                Iterator iterator = NonBlockingClient.selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) iterator.next();

                    System.out.println(String.format("[%s] Selector 호출 후 작업 가능한 키가 선택됨 , selectedKey : %s ", Thread.currentThread().getName(), selectionKey.channel()));

                    if(selectionKey.isReadable())
                        read(selectionKey);

                    iterator.remove();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void read(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

        System.out.println(String.format("[%s] %s 데이터 읽기 ", Thread.currentThread().getName(), socketChannel.toString()));
        int byteCount = socketChannel.read(byteBuffer);

        if (byteCount == -1) {
            throw new IOException();
        }

        byteBuffer.flip();
        String data = decoder.decode(byteBuffer).toString();
        System.out.println(String.format("[%s] %s 메세지 받음 - %s", Thread.currentThread().getName(), socketChannel.toString(), data));
        NonBlockingClient.clearBuffer(byteBuffer);
    }
}

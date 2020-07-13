package com.glamrock.example.nio.onethread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class OneThreadClient {
    public static void main(String[] args) {
        SocketChannel socketChannel = null;

        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);

            System.out.println(String.format("[%s] 연결 요청", Thread.currentThread().getName()));
            socketChannel.connect(new InetSocketAddress("localhost", 5001));
            System.out.println(String.format("[%s] 연결 성공", Thread.currentThread().getName()));

            ByteBuffer byteBuffer;
            Charset charset = Charset.forName("UTF-8");

            byteBuffer = charset.encode("Hello Server");
            socketChannel.write(byteBuffer);
            System.out.println(String.format("[%s] 데이터 보내기 성공", Thread.currentThread().getName()));

            byteBuffer = ByteBuffer.allocate(100);
            int byteCount = socketChannel.read(byteBuffer);
            byteBuffer.flip();

            String data = charset.decode(byteBuffer).toString();
            System.out.println(String.format("[%s] 데이터 받기 성공 : %s",Thread.currentThread().getName(), data));

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}

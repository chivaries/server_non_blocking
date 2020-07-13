package com.glamrock.example.nio.nonblocking;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Client {
    SocketChannel socketChannel;
    String sendData;

    Client(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;

        socketChannel.configureBlocking(false);
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);
    }

    void receive(SelectionKey selectionKey) {

    }
}

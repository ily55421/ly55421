package com.example.nettytest._01nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @author lin 2022/8/9 0:00
 */
public class Client {
    /**
     * 声明缓冲区
     */
    private final ByteBuffer send = ByteBuffer.allocate(1024);
    private final ByteBuffer receive = ByteBuffer.allocate(1024);
    private Selector selector;
    private SocketChannel channel;

    private String id;

    public Client(String id) throws Exception {
        this.id = id;
        this.channel = SocketChannel.open();
        this.channel.connect(new InetSocketAddress(InetAddress.getLocalHost(), 8080));
        this.channel.configureBlocking(false);

        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
    }

    public void handle() throws Exception {
        selector.select();

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            //TODO 标准做法 用完即删 防止事件重复
            iterator.remove();

            if (key.isReadable()) {
                showRsg(key);
            }
        }
//        sendMsg();
    }

    private void sendMsg() {
        try {
            while (true) {
                send.put((this.id + "/r/n").getBytes());
                send.flip();
                //
                while (send.hasRemaining()) {
                    channel.write(send);
                }
                send.compact();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    private void showRsg(SelectionKey key) {
        try {
            SocketChannel channel1 = (SocketChannel) key.channel();
            channel1.read(receive);
            receive.flip();
            String getData = Charset.forName("UTF-8").decode(receive).toString();
            System.out.println("服务器对我说:" + getData);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) throws Exception {

        for (int i = 0; i < 5; i++) {
            Client client = new Client("ID:" + i);
            //执行
            client.handle();
            Thread thread = new Thread(client::sendMsg);
            thread.start();

        }
    }
}

package com.example.nettytest._01nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @author lin 2022/8/8 23:45
 */
public class Server {

    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Selector selector;

    public Server() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        //是否为阻塞队列
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(8080));
        System.out.println("服务器已启动");

        selector = Selector.open();
        //注册管道
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void handler() throws Exception {
        while (true) {
            //等待事件的发生，这是一个阻塞的方法
            this.selector.select();

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                //是否是可接受的
                if (key.isAcceptable()) {
                    // 注册到read事件
                    ServerSocketChannel tmpChannel = (ServerSocketChannel)key.channel();
                    SocketChannel channel = tmpChannel.accept();
                    if (channel==null) {
                        continue;

                    }

                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);

                    // 发送欢迎的消息
                    ByteBuffer sendMsg = ByteBuffer.allocate(100);
                    sendMsg.put("hi 消息事件发送".getBytes());
                    // 翻转此缓冲区。限制设置为当前位置，然后位置设置为零。如果标记已定义，则将其丢弃。
                    // 重置消息标记
                    sendMsg.flip();
                    channel.write(sendMsg);
                }

                if (key.isReadable()) {
                    //读取数据
                    SocketChannel channel = (SocketChannel)key.channel();
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    String getData = Charset.forName("UTF-8").decode(buffer).toString();
                    System.out.println(getData);

                }
            }

        }


    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.handler();
    }

}

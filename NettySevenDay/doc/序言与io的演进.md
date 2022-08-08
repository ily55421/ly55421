# 序言与io的演进与示例代码

## 简介

![image-20220808223847366](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808223847.png)

![image-20220808223855607](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808223855.png)

**专注netty 教学**

性能优化 小窍门

![image-20220808224026291](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808224026.png)

Jboss

## Netty之序

![image-20220808224123543](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808224123.png)

## 本课程的目标

![image-20220808230106290](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230106.png)

# 从io开始

![image-20220808230215904](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230215.png)

## IO是个what？

![image-20220808230327313](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230327.png)

## IO阻塞模型

![image-20220808230409474](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230409.png)

## I/O劣势有哪些

![image-20220808230454850](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230454.png)

每一个socket 绑定一个 连接 影响线程复用

## 呼之欲出的NIO

![image-20220808230604210](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230604.png)

## Selector 多路复用技术

![image-20220808230648790](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230648.png)

## NIO之为什么选择netty

![image-20220808230723088](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230723.png)

**非阻塞线程模型**

## 小结

![image-20220808230816720](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230816.png)

nio 不会随着用户的增加 

# I/O的演进（中）

## Socket Demo

![image-20220808231310710](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808231310.png)

**accept 阻塞方法**

## Client

```JAVA
package com.example.nettytest._01socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author lin 2022/8/8 23:14
 */
public class Client {

    /**
     * 客户端连接
     *
     * @param id 用户标识
     * @throws IOException
     * @throws InterruptedException
     */
    public static void conn(String id) throws IOException, InterruptedException {
        int port = 8080;
        String ip = "127.0.0.1";
        Socket socket = new Socket(ip, port);
        new Thread((new Runnable() {
            private Socket mySocket;

            public Runnable setSocket(Socket socket) {
                this.mySocket = socket;
                return this;
            }

            @Override
            public void run() {
                try {
                    OutputStream out = mySocket.getOutputStream();
                    out.write(id.getBytes());
                    out.flush();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).setSocket(socket)).start();

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            try {
                conn("currUserID:" + i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

```

## Server

```JAVA
package com.example.nettytest._01socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lin 2022/8/8 23:15
 */
public class Server {
    private final static int port = 8080;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    /**
     * socket 阻塞方法启动
     *
     * @throws IOException
     */
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("接受一个请求");
            new Thread((new Runnable() {
                private Socket mySocket;

                public Runnable setSocket(Socket socket) {
                    this.mySocket = socket;
                    return this;
                }

                @Override
                public void run() {
                    InputStream in = null;
                    byte[] buf = new byte[100];
                    try {
                        in = this.mySocket.getInputStream();
                        //一直读取输入
                        while (true) {
                            int len = in.read(buf);
                            System.out.println(new String(buf, 0, len));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).setSocket(socket)).start();


        }


    }
}

```

## 输出结果 

```
接受一个请求
接受一个请求
currUserID:0
接受一个请求
currUserID:1
接受一个请求
接受一个请求
currUserID:2
currUserID:3
currUserID:4
java.net.SocketException: Connection reset
```









# I/O的演进（下）

![image-20220808230917409](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808230917.png)

## NIO DEMO 

**四种事件**

![image-20220808234020168](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/08/20220808234020.png)

connect 存储在 selector的key中

双全工  读写都可以操作

flip 把下标置为0获取当前缓存区中已有的数据

compact  分段读取 后续索引

clear 清空缓存区

## Client read模式

```JAVA
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

```

## Server 服务端

```JAVA
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

```

## 输出结果

```
客户端：
服务器对我说:hi 消息事件发送
服务器对我说:hi 消息事件发送
服务器对我说:hi 消息事件发送
服务器对我说:hi 消息事件发送
服务器对我说:hi 消息事件发送

服务端：
服务器已启动
ID:0/r/n
ID:1/r/n
ID:2/r/n
ID:3/r/n
ID:4/r/n
ID:4/r/n
ID:1/r/n
ID:2/r/n
ID:0/r/n
ID:3/r/n
ID:1/r/n
ID:2/r/n
ID:0/r/n
ID:4/r/n
```


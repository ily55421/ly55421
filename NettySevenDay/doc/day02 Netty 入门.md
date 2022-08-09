# Netty 入门

![image-20220809232013741](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/09/20220809232013.png)

## 传统nio 为什么没人使用

![image-20220809232044527](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/09/20220809232044.png)

## 搭建一个netty 服务器与客户端

![image-20220809232057310](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/09/20220809232057.png)

![image-20220809232148248](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/09/20220809232148.png)

![image-20220809232244206](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/09/20220809232244.png)

## 服务端

```JAVA
package com.example.nettytest._02nettysimple;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author lin 2022/8/9 22:45
 */
public class Server {


    public void startServer() throws Exception {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();


        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(
                            //SocketChannel 的管理
                            new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    socketChannel.pipeline().addLast(new Handler());
                                }
                            });

            ChannelFuture channelFuture = bootstrap.bind(8080).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();

            worker.shutdownGracefully();
        }
    }

    private static class Handler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            System.out.println(new String(bytes));
//            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startServer();

    }

}
```

## 客户端

```JAVA
package com.example.nettytest._02nettysimple;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author lin 2022/8/9 22:59
 */
public class Client {


    public void startClient() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();

        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //责任外包出去
                            socketChannel.pipeline().addLast(new ClientHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect("localhost", 8080).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }

    private static class ClientHandler extends ChannelHandlerAdapter  {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            new Thread(() -> {
                while (true) {
                    ByteBuf send = Unpooled.copiedBuffer("from client".getBytes());
                    ctx.writeAndFlush(send);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }


    }

    public static void main(String[] args) {
        Client client = new Client();
        client.startClient();

    }
}

```

## 备注 channelActive 为5.0版本特有方法 

```
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <!--            <version>4.1.25.Final</version>-->
            <version>5.0.0.Alpha2</version>
        </dependency>
```



## 效果
```
from client
from client
from client
from client
from client
from client
from client
from client
from client
from client
```


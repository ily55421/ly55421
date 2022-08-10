package com.example.nettytest._03;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.nio.charset.Charset;

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
                                    // netty 自带的编码
                                    socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                                    socketChannel.pipeline().addLast(new StringDecoder());
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
//            ByteBuf buf = (ByteBuf) msg;
//            byte[] bytes = new byte[buf.readableBytes()];
//            buf.readBytes(bytes);
//            String message = new String(bytes);
//            System.out.println("get once message here[[[[" + message + "]]]]");
            //get once message here[[[[from clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom client]]]]
            // 数据被粘成了一个
//            ctx.close();
            String message = msg.toString();
            System.out.println("get once message here[[[[" + message + "]]]]");
//            String txt = (String) msg;
            // io.netty.buffer.PooledUnsafeDirectByteBuf cannot be cast to java.lang.String 异常
            if (msg instanceof ByteBuf) {
                ByteBuf packet = (ByteBuf) msg;
                System.out.println("get once message here[[[[" +  packet.toString(Charset.defaultCharset()) + "]]]]");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startServer();

    }

}

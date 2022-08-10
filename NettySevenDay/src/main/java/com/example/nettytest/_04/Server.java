package com.example.nettytest._04;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
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
                                    ByteBuf tag = Unpooled.copiedBuffer("@_".getBytes());
                                    socketChannel.pipeline().addLast(new Handler());
                                    // netty 自带的编码  分隔符解码器
                                    socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,tag));
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

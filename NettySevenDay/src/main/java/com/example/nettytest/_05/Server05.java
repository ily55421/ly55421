package com.example.nettytest._05;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.nio.charset.Charset;

/**
 * @author lin 2022/8/9 22:45
 */
public class Server05 {


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
                                    // 定长解码器 长度是英文字母，abc长度为3，中文，一个汉字长度 3
                                    socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(23));
                                    socketChannel.pipeline().addLast(new StringDecoder());
                                    socketChannel.pipeline().addLast(new Handler());
                                }
                            });

            ChannelFuture channelFuture = bootstrap.bind(6666).sync();
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
        Server05 server = new Server05();
        server.startServer();

    }

}

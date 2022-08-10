package com.example.nettytest._05;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @author lin 2022/8/9 22:59
 */
public class Client05 {


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
                            // 定长解码器 长度是英文字母，abc长度为3，中文，一个汉字长度 3
                            socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(23));
                            socketChannel.pipeline().addLast(new StringDecoder());
                        }
                    });
            ChannelFuture future = bootstrap.connect("localhost", 6666).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }

    private static class ClientHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            new Thread(() -> {
                // 模拟粘包
                for (int i = 0; i < 100; i++) {
                    String msg = "from " + i;
                    ByteBuf send = Unpooled.copiedBuffer( fillBlock(msg).getBytes());
                    ctx.writeAndFlush(send);
                }
            }).start();
        }
    }

    /**
     * 字符补全
     *
     * @param str
     * @return
     */
    private static String fillBlock(String str) {
        String rs = "xxxxxxxxxxxxxxxxxxxx";
        if (str.length() >= 20) {
            return str;
        }
        //下标补齐
        return str + rs.substring(str.length(), rs.length())+"中";
    }

    public static void main(String[] args) {
        Client05 client = new Client05();
        client.startClient();
    }
}

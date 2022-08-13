package com.example.nettytest._08proto;

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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

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
                                    // 使用自定义的编解码类
                                    // 发送的时候  先执行handler 再执行 myEncode
                                    // 接收的时候  先执行MyDecode 在执行 Handle
                                    socketChannel.pipeline().addLast(new MyEncode());
                                    socketChannel.pipeline().addLast(new MyDecode());
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

    /**
     * 修改服务端的接收
     */
    private static class Handler extends ChannelHandlerAdapter {
        /**
         * 管道触发 执行方法
         *
         * @param ctx 管道处理上下文对象
         * @throws Exception 异常
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);

            HttpMsg.Response.Builder builder = HttpMsg.Response.newBuilder();
            builder.setName("hi client");
            builder.setSendTime(System.currentTimeMillis());
            ctx.writeAndFlush(builder.build());
        }

        /**
         * 管道读取
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            HttpMsg.Request request = (HttpMsg.Request) msg;
            System.out.printf(" form client msg ==>%s,time: %s  \n",request.getMsg(),request.getSendTime());

            for (int i = 0; i < request.getCommandListCount(); i++) {
                System.out.printf("command code is ==>%s \n", request.getCommandList(i));
            }

        }
    }

    /**
     * 自定义解码器
     */
    private static class MyDecode extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
            // 可能存在 len长度超过缓冲区的问题  没有进行解决
            int len = in.readUnsignedShort();
            ByteBuf buf = Unpooled.buffer(len);
            in.readBytes(buf);

            byte[] requestByteArray = buf.array();
            HttpMsg.Request request = HttpMsg.Request.parseFrom(requestByteArray);
            list.add(request);

        }
    }

    /**
     * 自定义编码器
     */
    private static class MyEncode extends MessageToByteEncoder<HttpMsg.Response> {

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, HttpMsg.Response o, ByteBuf byteBuf) throws Exception {
            int length = o.toByteArray().length;
            ByteBuf send = Unpooled.buffer(length + 2);
            send.writeInt(length);
            send.writeBytes(o.toByteArray());
//            channelHandlerContext.writeAndFlush(send);  只进行编码，不进行消息发送
            byteBuf.writeBytes(send);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startServer();

    }

}

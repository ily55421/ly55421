package com.example.nettytest._08protobufEncodeAndDecode;

import com.example.nettytest._08proto.HttpMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

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
                                    // 使用 protobuf  的编解码类
                                    socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                                    // 需要指定解码的类型
                                    socketChannel.pipeline().addLast(new ProtobufDecoder(HttpMsg.Request.getDefaultInstance()));
                                    socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                                    socketChannel.pipeline().addLast(new ProtobufEncoder());
                                    // 发送 处理 ==》编码
                                    // 接收 解码 ==> 处理
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
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            HttpMsg.Request request = (HttpMsg.Request) msg;
            System.out.printf(" this is server,there are some msg form client msg ==>%s,time: %s  \n", request.getMsg(), request.getSendTime());

            for (int i = 0; i < request.getCommandListCount(); i++) {
                System.out.printf("command code is ==>%s \n", request.getCommandList(i));
            }

        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startServer();

    }

}

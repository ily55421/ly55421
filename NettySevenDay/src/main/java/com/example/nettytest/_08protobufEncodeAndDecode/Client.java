package com.example.nettytest._08protobufEncodeAndDecode;

import com.example.nettytest._08proto.HttpMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

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
                            // 使用 protobuf  的编解码类
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            // 需要指定解码的类型
                            socketChannel.pipeline().addLast(new ProtobufDecoder(HttpMsg.Response.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            // 发送 处理 ==》编码
                            // 接收 解码 ==> 处理
                            socketChannel.pipeline().addLast(new ClientHandler());

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
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 接收数据
            HttpMsg.Response response = (HttpMsg.Response) msg;
            System.out.printf("this is client,there are some msg  form serve msg ==>%s,time: %s  \n", response.getName(), response.getSendTime());

            // 发送数据
            HttpMsg.Request.Builder builder = HttpMsg.Request.newBuilder();
            builder.setMsg("hi,serve");
            builder.addCommandList("select * 测试");
            builder.addCommandList("select * 测试22222");
            builder.setSendTime(((int) System.currentTimeMillis()));
            ctx.writeAndFlush(builder.build());
        }
    }



    public static void main(String[] args) {
        Client client = new Client();
        client.startClient();
    }
}

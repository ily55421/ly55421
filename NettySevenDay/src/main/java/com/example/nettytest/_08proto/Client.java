package com.example.nettytest._08proto;

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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

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

                            // 使用自定义编解码类
                            socketChannel.pipeline().addLast(new MyEncode());
                            socketChannel.pipeline().addLast(new MyDecode());
                            //责任外包出去
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
            System.out.printf("form serve msg ==>%s,time: %s  \n", response.getName(), response.getSendTime());

            // 发送数据
            HttpMsg.Request.Builder builder = HttpMsg.Request.newBuilder();
            builder.setMsg("hi,serve");
            builder.addCommandList( "select * 测试");
            builder.addCommandList( "select * 测试22222");
            builder.setSendTime(((int) System.currentTimeMillis()));
            ctx.writeAndFlush(builder.build());
        }
    }

    /**
     * 自定义解码器   解码的是相应
     */
    private static class MyDecode extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
            int len = in.readUnsignedShort();
            ByteBuf buf = Unpooled.buffer(len);
            in.readBytes(buf);

            byte[] requestByteArray = buf.array();
            HttpMsg.Response request = HttpMsg.Response.parseFrom(requestByteArray);
            list.add(request);

        }
    }

    /**
     * 自定义编码器   发送的是  request
     */
    private static class MyEncode extends MessageToByteEncoder<HttpMsg.Request> {

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, HttpMsg.Request o, ByteBuf byteBuf) throws Exception {
            int length = o.toByteArray().length;
            ByteBuf send = Unpooled.buffer(length + 2);
            send.writeInt(length);
            send.writeBytes(o.toByteArray());
//            channelHandlerContext.writeAndFlush(send);  只进行编码，不进行消息发送
            byteBuf.writeBytes(send);
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
        return str + rs.substring(str.length(), rs.length()) + "中";
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.startClient();
    }
}

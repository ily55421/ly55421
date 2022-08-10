package com.example.group.server;

import com.example.group.decoder.MyDecoder;
import com.example.group.encoder.MyEncoder;
import com.example.group.ClientChatGroupHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @Author: linK
 * @Date: 2022/8/10 15:33
 * @Description TODO
 */
public class ClientGroupService {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
        try {
            //创建bootstrap对象，配置参数
            Bootstrap bootstrap = new Bootstrap();
            //设置线程组
            bootstrap.group(eventExecutors)
                    //设置客户端的通道实现类型
                    .channel(NioSocketChannel.class)
                    //使用匿名内部类初始化通道
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            //1.拆包器
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 5, 4));
                            //2.自定义解码器
                            ch.pipeline().addLast(new MyDecoder());
                            //3.自定义业务  添加客户端通道的处理器
                            ch.pipeline().addLast(new ClientChatGroupHandler());
                            //4.自定义编码器
                            ch.pipeline().addLast(new MyEncoder());
                        }
                    });
            System.out.println("客户端准备就绪，随时可以起飞~");
            //连接服务端
            ChannelFuture channelFuture = bootstrap.connect("localhost", 8145).sync();
            //对通道 进行监听  返回当此通道关闭时将通知的ChannelFuture 。此方法始终返回相同的 Future实例。
            channelFuture.channel().closeFuture().sync();
        } finally {
            //关闭线程组
            eventExecutors.shutdownGracefully();
        }
    }

}

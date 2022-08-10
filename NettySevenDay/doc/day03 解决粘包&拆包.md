# day03 解决粘包&拆包

![image-20220810222642448](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810222642.png)

**每秒钟发送一次心跳**

![image-20220810222710373](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810222710.png)

## 粘包&拆包

![image-20220810222805844](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810222805.png)

**拆包**  一个数据包超出缓存空间  会被拆分成两个 

**粘包**  一个缓存空间同时发送了两条数据 会被误认为是同一条数据

**模拟效果**  修改ke

```JAVA
    private static class ClientHandler extends ChannelHandlerAdapter  {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            new Thread(() -> {
                // 模拟粘包
                for (int i = 0; i < 100; i++) {

//                } (true) {
                    ByteBuf send = Unpooled.copiedBuffer("from client".getBytes());
                    ctx.writeAndFlush(send);
//                    try {
//                        Thread.sleep(1000);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
            }).start();
        }
    }
```

**粘包和拆包问题**

```java
    private static class Handler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String message = new String(bytes);
            System.out.println("get once message here[[[[" + message + "]]]]");
            //get once message here[[[[from clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom clientfrom client]]]]
            // 数据被粘成了一个
//            ctx.close();
        }
    }

```

## netty自带的编码解码工具类 添加责任链

![image-20220810224132366](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810224132.png)

```JAVA
 // netty 自带的编码
                                    socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                                    socketChannel.pipeline().addLast(new StringDecoder());
```

**添加责任链**

```JAVA
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
```

## **消息处理**

```JAVA
    private static class Handler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String message = msg.toString();
            // 对数据进行强转之后 接受 d
            String txt = (String) msg;
            System.out.println("get once message here[[[[" + message + "]]]]");
            System.out.println("get once message here[[[[" + txt + "]]]]");
        }
    }
```





## 解决方案

![image-20220810224352340](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810224352.png)







## 小结

![image-20220810224108527](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810224108.png)

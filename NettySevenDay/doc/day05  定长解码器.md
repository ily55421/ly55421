# day05  定长解码器

![image-20220810233148159](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233148.png)

![image-20220810233156224](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233156.png)

## 定长解码器原理

![image-20220810233307310](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233307.png)

![image-20220810233354060](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233354.png)

长度正好等于缓存区的空间



## 设计

![image-20220810233415773](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233415.png)

## 实战

![image-20220810233431182](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/10/20220810233431.png)

## 修改服务端 解码为FixedLengthFrameDecoder

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
```

## 客户端字符补全 解决单条消息不足缓存空间

```JAVA
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
```


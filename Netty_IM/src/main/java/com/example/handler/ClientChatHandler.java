package com.example.handler;

import com.example.entity.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.Scanner;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:42
 * @Description TODO 客户端处理器
 */
public class ClientChatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //通道就绪时，发起登录请求
        login(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //根据msg做类型判断，不同的业务做不同的处理
        if(msg instanceof LoginResBean){
            //1.登录结果响应
            LoginResBean res=(LoginResBean) msg;
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>登录响应："+res.getMsg());
            if(res.getStatus()==0){
                //1.登录成功，则给通道绑定属性
                ctx.channel().attr(AttributeKey.valueOf("userid")).set(res.getUserid());
                //2.调用发送消息方法
                sendMsg(ctx.channel());
            }else{
                //1.登录失败，调用登录方法
                login(ctx.channel());
            }

        }else if(msg instanceof MsgResBean){
            //1.发送消息结果响应
            MsgResBean res=(MsgResBean)msg;
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>发送响应："+res.getMsg());

        }else if(msg instanceof MsgRecBean){
            //2.接受消息
            MsgRecBean res=(MsgRecBean)msg;
            System.out.println("fromuserid="+res.getFromuserid()+",msg="+res.getMsg());
        }
    }

    //登录方法
    private void login(Channel channel){
        Scanner scanner=new Scanner(System.in);
        System.out.println(">>用户ID：");
        Integer userid=scanner.nextInt();
        System.out.println(">>用户名称：");
        String username=scanner.next();

        LoginReqBean bean=new LoginReqBean();
        bean.setUserid(userid);
        bean.setUsername(username);
        channel.writeAndFlush(bean);
    }

    //发送消息方法
    private void sendMsg(final Channel channel){
        final Scanner scanner=new Scanner(System.in);
        new Thread(new Runnable() {
            public void run() {
                while (true){
                    System.out.println(">>接收人ID：");
                    Integer touserid=scanner.nextInt();
                    System.out.println(">>聊天内容：");
                    String msg=scanner.next();

                    MsgReqBean bean=new MsgReqBean();
                    //从通道属性获取发送人ID
                    Integer fromuserid=(Integer) channel.attr(
                            AttributeKey.valueOf("userid")
                    ).get();
                    //发送人ID
                    bean.setFromuserid(fromuserid);
                    //接受人ID
                    bean.setTouserid(touserid);
                    //发送消息
                    bean.setMsg(msg);
                    channel.writeAndFlush(bean);
                }
            }
        }).start();
    }
}
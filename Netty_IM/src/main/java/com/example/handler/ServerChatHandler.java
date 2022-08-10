package com.example.handler;

import com.example.entity.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: linK
 * @Date: 2022/8/9 15:07
 * @Description TODO
 */
public class ServerChatHandler extends ChannelInboundHandlerAdapter {
    //1.定义一个Map（key是用户ID，value是连接通道）
    private static Map<Integer, Channel> map=new HashMap<Integer, Channel>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof LoginReqBean){
            //1.登录请求
            login((LoginReqBean) msg,ctx.channel());

        }else if(msg instanceof MsgReqBean){

            //2.发送消息请求
            sendMsg((MsgReqBean)msg,ctx.channel());
        }
    }
    //登录处理方法
    private void login(LoginReqBean bean, Channel channel){
        LoginResBean res=new LoginResBean();

        //从map里面根据用户ID获取连接通道
        Channel c=map.get(bean.getUserid());

        if(c==null){
            //通道为空，证明该用户没有在线

            //1.添加到map
            map.put(bean.getUserid(),channel);
            //2.给通道赋值
            channel.attr(AttributeKey.valueOf("userid")).set(bean.getUserid());
            //3.响应
            res.setStatus(0);
            res.setMsg("登录成功");
            res.setUserid(bean.getUserid());
            channel.writeAndFlush(res);
        }else{
            //通道不为空，证明该用户已经在线了

            res.setStatus(1);
            res.setMsg("该账户目前在线");
            channel.writeAndFlush(res);
        }
    }
    //消息发送处理方法
    private void sendMsg(MsgReqBean bean, Channel channel){
        Integer touserid=bean.getTouserid();
        Channel c=map.get(touserid);

        if(c==null){
            MsgResBean res=new MsgResBean();
            res.setStatus(1);
            res.setMsg(touserid+",不在线");
            channel.writeAndFlush(res);

        }else{
            MsgRecBean res=new MsgRecBean();
            res.setFromuserid(bean.getFromuserid());
            res.setMsg(bean.getMsg());
            c.writeAndFlush(res);
        }
    }
}
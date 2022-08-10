package com.example.decoder;

import com.alibaba.fastjson.JSON;
import com.example.entity.BaseBean;
import com.example.util.MapUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:34
 * @Description TODO 解码实现
 */
public class MyDecoder extends ByteToMessageDecoder {
    protected void decode(
            ChannelHandlerContext channelHandlerContext,
            ByteBuf byteBuf,
            List<Object> list) throws Exception {

        //1.根据协议取出数据
        int tag=byteBuf.readInt();
        //标识符
        byte code=byteBuf.readByte();
        //获取指令
        int len=byteBuf.readInt();
        //获取数据长度
        byte[] bytes=new byte[len];
        byteBuf.readBytes(bytes);

        //2.根据code获取类型
        Class<? extends BaseBean> c= MapUtils.getBean(code);

        //3.反序列化
        BaseBean baseBean= JSON.parseObject(bytes,c);

        list.add(baseBean);
    }
}
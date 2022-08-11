package com.example.nettytest._06packtest;

import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author lin 2022/8/11 22:27
 * 序列化对象测试
 */
public class MsgPackObjectTest {
    /**
     * 测试java自带序列化工具
     *
     * @param rs 序列化参数
     * @throws IOException
     */
    public static void java(List<UserInfo> rs) throws Exception {
        long time = System.currentTimeMillis();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
        out.writeObject(rs);
        out.flush();
        out.close();

        byte[] arr = byteArrayOutputStream.toByteArray();
        System.out.println("java size:" + arr.length);

        ByteArrayInputStream bin = new ByteArrayInputStream(arr);
        ObjectInputStream in = new ObjectInputStream(bin);

        List<UserInfo> newList = (List<UserInfo>) in.readObject();

        System.out.println("反序列化成功，list长度为：" + newList.size());
        System.out.println("time cost ==>" + (System.currentTimeMillis() - time));
    }

    /**
     * 使用msgpack 测试序列化
     */
    public static void msgpack(List<UserInfo> rs) throws Exception {
        long time = System.currentTimeMillis();

        MessagePack pack = new MessagePack();
        byte[] arr = pack.write(rs);
        System.out.println("messagePack size:" + arr.length);

        // 指定对象序列化
        List<UserInfo> newList = pack.read(arr, Templates.tList(UserTemplate.getInstance()));
        System.out.println(((UserInfo) newList.get(0)));
        System.out.println("反序列化成功，list长度为：" + newList.size());
        System.out.println("time cost ==>" + (System.currentTimeMillis() - time));

    }


    private static List<UserInfo> createData(int size) {
        List<UserInfo> data = new ArrayList<>();
        IntStream.rangeClosed(0, size).boxed().forEach(m -> {
            data.add(new UserInfo("dsafksangasn", m));
        });
        return data;
    }

    public static void main(String[] args) throws Exception {
        List<UserInfo> data = createData(1000);
        java(data);
        // Exception in thread "main" java.io.NotSerializableException: com.example.nettytest._06packtest.UserInfo
        // 原生序列化工具会报错    需要 实现序列化接口
        msgpack(data);
        // Exception in thread "main" org.msgpack.MessageTypeException: Cannot find template for class com.example.nettytest._06packtest.UserInfo class.
        // Try to add @Message annotation to the class or call MessagePack.register(Type).
        // 转换异常  需要定义模板类型
        // Cannot find template for class com.example.nettytest._06packtest.UserInfo class.  Try to add @Message annotation to the class or call MessagePack.register(Type).
        // 实体类上 加上  @Message 注解



    }
}

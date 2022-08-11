package com.example.nettytest._06packtest;

import org.msgpack.MessagePack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author lin 2022/8/11 22:27
 */
public class MsgPackTest {
    /**
     * 测试java自带序列化工具
     *
     * @param rs 序列化参数
     * @throws IOException
     */
    public static void java(List<String> rs) throws Exception {
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

        List<String> newList = (List<String>) in.readObject();

        System.out.println("反序列化成功，list长度为：" + newList.size());
        System.out.println("time cost ==>" + (System.currentTimeMillis() - time));
    }

    /**
     * 使用msgpack 测试序列化
     */
    public static void msgpack(List<String> rs) throws Exception {
        long time = System.currentTimeMillis();

        MessagePack pack = new MessagePack();
        byte[] arr = pack.write(rs);
        System.out.println("messagePack size:" + arr.length);
        //Templates.tList(Templates.TString)  定义模板
//        List<String> newList = pack.read(arr, Templates.tList(Templates.TString));

        // 简单类型支持强转
        List<String> newList = (List<String>) pack.read(arr);
        System.out.println("反序列化成功，list长度为：" + newList.size());
        System.out.println("time cost ==>" + (System.currentTimeMillis() - time));

    }


    private static List<String> createData(int size) {
        List<String> data = new ArrayList<>();
        IntStream.rangeClosed(0, size).boxed().forEach(m -> {
            data.add("dsafksangasn" + m);
        });
        return data;
    }

    public static void main(String[] args) throws Exception {
        List<String> data = createData(1000);
        java(data);
        msgpack(data);
        //java size:17967
        //反序列化成功，list长度为：1001
        //time cost ==>11
        //messagePack size:15910
        //反序列化成功，list长度为：1001
        //time cost ==>45


        List<String> dataTwo = createData(100000);
        java(dataTwo);
        msgpack(dataTwo);
        //java size:1988969
        //反序列化成功，list长度为：100001
        //time cost ==>47
        //messagePack size:1788914
        //反序列化成功，list长度为：100001
        //time cost ==>31

        List<String> dataThree = createData(1000000);
        java(dataThree);
        msgpack(dataThree);
        //反序列化成功，list长度为：1000001
        //time cost ==>877
        //messagePack size:18888915
        //反序列化成功，list长度为：1000001
        //time cost ==>156    数据量大的时候差异比较明显
    }
}

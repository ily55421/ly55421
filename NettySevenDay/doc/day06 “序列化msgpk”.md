# day06 “序列化msgpk上”

![image-20220811222533250](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811222533.png)

![image-20220811222547455](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811222547.png)

## 序列化是干嘛的

原生序列化 跨语言

![image-20220811222646990](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811222647.png)

## 序列化技术都有哪些

![image-20220811225345020](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811225345.png)

![image-20220811225411179](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811225411.png)

## 实战

```xml-dtd
<!--        msgpack 序列化-->
        <dependency>
            <groupId>org.msgpack</groupId>
            <artifactId>msgpack</artifactId>
            <version>0.6.12</version>
        </dependency>

        <dependency>
            <groupId>org.javassist</groupId>
            <artifactId>javassist</artifactId>
            <version>3.25.0-GA</version>
        </dependency>
```

**示例代码**

```JAVA
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
```

## 序列化后的数据

![image-20220811225655785](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811225655.png)

反序列化后的数据

![image-20220811225753438](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811225753.png)

## JAVA序列化对象

实体类实现序列化接口即可

## msgpack对复杂对象的序列化

### 实体类上加@Message

```java
package com.example.nettytest._06packtest;

import org.msgpack.annotation.Message;

import java.io.Serializable;

/**
 * @author lin 2022/8/11 22:58
 */
@Message
public class UserInfo implements Serializable {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserInfo() {
    }

    public UserInfo(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

### 定义模板类  

```JAVA
package com.example.nettytest._06packtest;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * @author lin 2022/8/11 23:08
 */
public class UserTemplate extends AbstractTemplate<UserInfo> {
    /**
     *
     * @param packer  打包的对象
     * @param userInfo 序列化的对象
     * @param required 是否必要
     * @throws IOException
     */
    @Override
    public void write(Packer packer, UserInfo userInfo, boolean required) throws IOException {
        if (userInfo == null) {
            // 对象为空 且是必须的参数
            if (required) {
                throw new MessageTypeException("Attempted to write null");
            } else {
                packer.writeNil();
                return;
            }
        } else {
            packer.write(userInfo);
            return;
        }
    }

    @Override
    public UserInfo read(Unpacker unpacker, UserInfo userInfo, boolean required) throws IOException {
        //非必要  且为空
        if (!required && unpacker.trySkipNil()) {
            return null;
        } else {
            return unpacker.read(UserInfo.class);
        }

    }

    /**
     * 序列化模板对象
     */
    private static final UserTemplate INSTANCE = new UserTemplate();

    public static UserTemplate getInstance() {
        return INSTANCE;
    }
}

```

### 序列化强更改为模板转换

```JAVA
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

```

### 序列化后效果



![image-20220811232244619](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/11/20220811232244.png)
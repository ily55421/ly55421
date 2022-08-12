# day07 序列化之protobuf

![image-20220812230542174](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812230542.png)

![image-20220812230547104](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812230547.png)

## protobuf 生产中更偏向于实战

![image-20220812231306542](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231306.png)

## Pojo是什么

![image-20220812231402063](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231402.png)

## Proto语法的示意

![image-20220812231505342](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231505.png)

会生成一个内部类

![image-20220812231544702](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231544.png)

## Protobuf类型对照

![image-20220812231701432](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231701.png)

## Protobuf 实战

![image-20220812232728105](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812232728.png)

### protobuf插件

![image-20220812231919102](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812231919.png)

![image-20220812232809535](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812232809.png)

生成序列化结构体

![image-20220812233603468](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812233603.png)

![image-20220812233617563](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/12/20220812233617.png)

### Proto文件

```
syntax = "proto3"; // proto3 必须加此注解
//生成文件所在包名
option java_package = "com.example.nettytest._07Protobuf.pojo";
//生成的java文件名
option java_outer_classname = "School";

//生成类 相当于内部类 School.Student
message Student {
  string name = 1;
  int32 age = 2;
  int32 phone = 3;
}


message ClassRoom{
    int32 id = 1;
    string name = 2;

}
```

### test代码

```java
package com.example.nettytest._07Protobuf;

import com.example.nettytest._07Protobuf.pojo.School;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author lin 2022/8/12 23:51
 */
public class ProtobufTest {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        School.Student.Builder builder = School.Student.newBuilder();
        School.Student student = builder.setAge(1).setName("ceSH").setPhone(132).build();
        //序列化
        byte[] buf = student.toByteArray();
        System.out.println("序列化完毕,size=" + buf.length);

        //反序列化
        School.Student student1 = School.Student.parseFrom(buf);
        System.out.println(student1.getName()+student1.getAge());

        //序列化完毕,size=11
        //ceSH1
    }
}
```

![image-20220813000149016](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/13/20220813000149.png)

```
@echo off
for %%i in (proto/*.proto) do(
    protoc ./proto/%%i --java_out = ./java
    echo exchange %%i To java file successFully!
)
pause
```

### 命令行执行

![image-20220813000554441](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/13/20220813000554.png)

## proto 复杂对象序列化

```protobuf
syntax = "proto3"; // proto3 必须加此注解
//生成文件所在包名
option java_package = "com.example.nettytest._07Protobuf.pojo";
//生成的java文件名
option java_outer_classname = "Company";

//生成类 相当于内部类 Company.Employee
message Employee {
  string name = 1;
  int32 age = 2;
  int32 phone = 3;
  // 相当于List<Work>
  repeated Work works = 4;
}


message Work{
  int32 id = 1;
  string name = 2;
  string handle = 3;

}
```

**执行命令**

```
protoc -I=./ --java_out=./ ./Complex.proto


```



**序列化测试**

```JAVA
package com.example.nettytest._07Protobuf;

import com.example.nettytest._07Protobuf.pojo.Company;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author lin 2022/8/13 0:09
 * 测试 protobuf 复杂对象序列化
 */
public class CompanyTest {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        Company.Employee.Builder builder = Company.Employee.newBuilder();
        Company.Employee employee = builder.setAge(1).
                setName("员工")
                .setPhone(12122)
                .addWorks(Company.Work.newBuilder().setId(1).setName("222").setHandle("处理1"))
                .addWorks(Company.Work.newBuilder().setId(2).setName("222").setHandle("处理2"))
                .addWorks(Company.Work.newBuilder().setId(3).setName("222").setHandle("处理3")).build();

        byte[] buf = employee.toByteArray();
        System.out.println("序列化完毕，size=" + buf.length);

        Company.Employee employee1 = Company.Employee.parseFrom(buf);
        System.out.println(employee1.getName()+employee1.getWorks(1).getHandle());
        //序列化完毕，size=67
        //员工处理2
    }

}

```

![image-20220813001722239](https://linkeq.oss-cn-chengdu.aliyuncs.com/image/2022/08/13/20220813001722.png)

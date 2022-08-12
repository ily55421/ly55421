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

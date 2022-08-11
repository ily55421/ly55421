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

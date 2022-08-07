package com.design.mediatorPattern;

public class User {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User(String name) {
        this.name = name;
    }

    /**
     * 发送消息 调用中介方法
     *
     * @param message 消息体
     */
    public void sendMessage(String message) {
        ChatRoom.showMessage(this, message);
    }
}
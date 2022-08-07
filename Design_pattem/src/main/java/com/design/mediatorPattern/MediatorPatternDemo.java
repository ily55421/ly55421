package com.design.mediatorPattern;

public class MediatorPatternDemo {
    public static void main(String[] args) {
        User robert = new User("Robert");
        User john = new User("John");
        // 通过消息房间中介 来发送消息
        robert.sendMessage("Hi! John!");
        john.sendMessage("Hello! Robert!");
        //Sun Aug 07 22:24:05 CST 2022 [Robert] : Hi! John!
        //Sun Aug 07 22:24:05 CST 2022 [John] : Hello! Robert!
    }
}
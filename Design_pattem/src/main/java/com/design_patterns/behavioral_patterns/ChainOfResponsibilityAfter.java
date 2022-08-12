package com.design_patterns;

import java.util.Random;

/**
 * @Author: linK
 * @Date: 2022/8/10 10:58
 * @Description TODO The client submits each request to the "chain" abstraction and is decoupled from all subsequent processing.
 * 客户端将所有请求交给了链 抽象,并与所有后续处理解耦。
 */
public class ChainOfResponsibilityAfter { public static void main(String[] args) {
    HandlerAfter rootChain = new HandlerAfter();
    // 单链节点添加
    rootChain.add(new HandlerAfter());
    rootChain.add(new HandlerAfter());
    rootChain.add(new HandlerAfter());
    //循环单链
    rootChain.wrapAround(rootChain);
    for (int i = 1; i < 6; i++) {
        System.out.println("Operation #" + i + ":");
        // 链式调用
        rootChain.execute(i);
        System.out.println();
    }
    //Operation #1:
    //   1-busy
    //   2-busy
    //   3-busy
    //   4-busy
    //   1-busy
    //   2-busy
    //   3-busy
    //   4-busy
    //1-handled-1
    //
    //Operation #2:
    //   1-busy
    //   2-busy
    //3-handled-2
    //
    //Operation #3:
    //   1-busy
    //   2-busy
    //   3-busy
    //4-handled-3
    //
    //Operation #4:
    //   1-busy
    //2-handled-4
    //
    //Operation #5:
    //   1-busy
    //2-handled-5
}
    
}
class HandlerAfter {
    private final static Random RANDOM = new Random();
    private static int nextID = 1;
    private int id = nextID++;
    private HandlerAfter nextInChain;

    public void add(HandlerAfter next) {
        if (nextInChain == null) {
            // 添加节点
            nextInChain = next;
        } else {
            // 向后遍历节点
            nextInChain.add(next);
        }
    }
    // 环绕
    public void wrapAround(HandlerAfter root) {
        if (nextInChain == null) {
            // 添加根节点
            nextInChain = root;
        } else {
            // 将头节点 作为尾节点的后后继节点
            nextInChain.wrapAround(root);
        }
    }

    public void execute(int num) {
        if (RANDOM.nextInt(4) != 0) {
            System.out.println("   " + id + "-busy  ");
            // 下一个节点执行吧 循环链执行 直到程序终止
            nextInChain.execute(num);
        } else {
            System.out.println(id + "-handled-" + num);
        }
    }
}


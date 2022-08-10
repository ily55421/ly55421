package com.design_patterns;

import java.util.Random;

/**
 * @Author: linK
 * @Date: 2022/8/10 10:58
 * @Description TODO The client is responsible for stepping through the "list" of Handler objects,
 * and determining when the request has been handled.
 * //客户端负责单步执行 Handler 对象的“列表”，并确定何时处理了请求。
 */
public class ChainOfResponsibilityBefore {
    public static void main(String[] args) {
        Handler[] nodes = {new Handler(), new Handler(),
                new Handler(), new Handler()};
        // 当同一个请求有多个不同处理规则   可以添加责任链
        for (int i = 1, j; i < 6; i++) {
            System.out.println("Operation #" + i + ":");
            j = 0;
            while (!nodes[j].execute(i)) {
                j = (j + 1) % nodes.length;
            }
            System.out.println();
        }
    }
}
class Handler {
    private static final Random RANDOM = new Random();
    private static int nextID = 1;
    private int id = nextID++;

    public boolean execute(int num) {
        // 不处理   规则处理
        if (RANDOM.nextInt(4) != 0) {
            System.out.println("   " + id + "-busy  ");
            return false;
        }
        // 处理
        System.out.println(id + "-handled-" + num);
        return true;
        //Operation #1:
        //   1-busy
        //   2-busy
        //3-handled-1
        //
        //Operation #2:
        //   1-busy
        //2-handled-2
        //
        //Operation #3:
        //1-handled-3
        //
        //Operation #4:
        //   1-busy
        //2-handled-4
        //
        //Operation #5:
        //1-handled-5
    }
}


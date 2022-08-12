package com.design_patterns;

import java.util.Random;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:02
 * @Description TODO
 */
public class Bridge_in_Java_Another {
    public static void main(String[] args) {
        Stack02[] stacks = {new Stack02("array"), new Stack02("list"),
                new Stack02FIFO(), new Stack02Hanoi()};
        for (int i=1, num; i < 15; i++) {
            for (int j=0; j < 3; j++) {
                stacks[j].push( i );
            }
        }
        Random rn = new Random();
        for (int i=1, num; i < 15; i++) {
            stacks[3].push(rn.nextInt(20));
        }
        for (int i=0, num; i < stacks.length; i++) {
            while (!stacks[i].isEmpty()) {
                System.out.print(stacks[i].pop() + "  ");
            }
            System.out.println();
        }
        System.out.println("total rejected is " + ((Stack02Hanoi)stacks[3]).reportRejected());
        //12  11  10  9  8  7  6  5  4  3  2  1
        //14  13  12  11  10  9  8  7  6  5  4  3  2  1
        //1  2  3  4  5  6  7  8  9  10  11  12
        //0  1  8
        //total rejected is 11
    }
}

/**
 * 节点定义
 */
class Node02 {
    public int value;
    public Node02 prev, next;

    public Node02(int i) {
        value = i;
    }
}

/**
 * 基础栈 base class
 */
class Stack02 {
    private Stack02Impl impl;

    /**
     * 动态构造
     * @param s
     */
    public Stack02( String s ) {
        if (s.equals("array")) {
            impl = new Stack02Array();
        } else if (s.equals("list")) {
            impl = new Stack02List();
        } else {
            System.out.println("Stack02: unknown parameter");
        }
    }

    /**
     * 初始基于数组实现
     */
    public Stack02() {
        this("array");
    }

    public void push(int in) {
        impl.push( in );
    }

    public int pop() {
        return impl.pop();
    }

    public int top() {
        return impl.top();
    }

    public boolean isEmpty() {
        return impl.isEmpty();
    }

    public boolean isFull() {
        return impl.isFull();
    }
}

class Stack02Hanoi extends Stack02 {
    private int totalRejected = 0;

    public Stack02Hanoi() {
        super("array");
    }

    public Stack02Hanoi(String s) {
        super(s);
    }

    public int reportRejected() {
        return totalRejected;
    }

    public void push(int in) {
        if (!isEmpty() && in > top()) {
            totalRejected++;
        }
        else {
            super.push(in);
        }
    }
}

class Stack02FIFO extends Stack02 {
    private Stack02Impl stackImpl = new Stack02List();

    public Stack02FIFO() {
        super("array");
    }

    public Stack02FIFO(String s) {
        super(s);
    }

    public int pop() {
        while (!isEmpty()) {
            stackImpl.push(super.pop());
        }
        int ret = stackImpl.pop();
        while (!stackImpl.isEmpty()) {
            push(stackImpl.pop());
        }
        return ret;
    }
}

interface Stack02Impl {
    void push(int i);
    int pop();
    int top();
    boolean isEmpty();
    boolean isFull();
}

/**
 * Stack02Array 基于数组实现的栈
 */
class Stack02Array implements Stack02Impl {
    private int[] items;
    private int total = -1;

    public Stack02Array() {
        this.items = new int[12];
    }

    public Stack02Array(int cells) {
        this.items = new int[cells];
    }

    public void push(int i) {
        if (!isFull()) {
            items[++total] = i;
        }
    }

    public boolean isEmpty() {
        return total == -1;
    }

    public boolean isFull() {
        return total == items.length - 1;
    }

    public int top() {
        if (isEmpty()) {
            return -1;
        }
        return items[total];
    }

    public int pop() {
        if (isEmpty()) {
            return -1;
        }
        return items[total--];
    }
}

/**
 * 基于双向链表 实现的栈
 */
class Stack02List implements Stack02Impl {
    /**
     * 栈的根节点
     */
    private Node02 last;

    public void push(int i) {
        if (last == null) {
            last = new Node02(i);
        } else {
            last.next = new Node02(i);
            last.next.prev = last;
            last = last.next;
        }
    }

    public boolean isEmpty() {
        return last == null;
    }

    public boolean isFull() {
        return false;
    }

    public int top() {
        if (isEmpty()) {
            return -1;
        }
        return last.value;
    }

    public int pop() {
        if (isEmpty()) {
            return -1;
        }
        int ret = last.value;
        last = last.prev;
        return ret;
    }
}


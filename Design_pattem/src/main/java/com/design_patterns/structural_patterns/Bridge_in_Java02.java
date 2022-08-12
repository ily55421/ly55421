package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:08
 * @Description TODO
 */
public class Bridge_in_Java02 {
    public static void main(String[] args) {
        Stack03[] stacks = {new Stack03("java"), new Stack03("mine"),
                new Stack03Hanoi02("java"), new Stack03Hanoi02("mine")};
        for (int i = 0, num; i < 20; i++) {
            num = (int) (Math.random() * 1000) % 40;
            for (Stack03 stack : stacks) {
                stack.push(num);
            }
        }
        for (int i = 0, num; i < stacks.length; i++) {
            while (!stacks[i].isEmpty()) {
                System.out.print(stacks[i].pop() + "  ");
            }
            System.out.println();
        }
        System.out.println("total rejected is " + ((Stack03Hanoi02) stacks[3]).reportRejected());
        //23  36  36  33  10  30  20  39  32  11  1  12  6  15  20  25  3  6  37  14
        //23  36  36  33  10  30  20  39  32  11  1  12  6  15  20  25  3  6  37  14
        //1  3  6  14
        //1  3  6  14
        //total rejected is 16
    }
}

/**
 * Create an interface/wrapper class that "has a"
 * implementation object and delegates all requests to it
 */
class Stack03 {
    protected Stack03Imp imp;

    public Stack03(String s) {
        if (s.equals("java")) {
            imp = new Stack03Java();
        } else {
            imp = new Stack03Mine();
        }
    }

    public Stack03() {
        this("java");
    }

    public void push(int in) {
        imp.push(in);
    }

    public int pop() {
        return (Integer) imp.pop();
    }

    public boolean isEmpty() {
        return imp.empty();
    }
}

/**
 * Embellish the interface class with derived classes if desired
 */
class Stack03Hanoi02 extends Stack03 {
    private int totalRejected = 0;

    public Stack03Hanoi02() {
        super("java");
    }

    public Stack03Hanoi02(String s) {
        super(s);
    }

    public int reportRejected() {
        return totalRejected;
    }

    public void push(int in) {
        if (!imp.empty() && in > (Integer) imp.peek()) {
            totalRejected++;
        } else {
            imp.push(in);
        }
    }
}

/**
 * Create an implementation/body base class
 */
interface Stack03Imp {
    Object push(Object o);

    Object peek();

    boolean empty();

    Object pop();
}

//调用系统栈的实现
class Stack03Java extends java.util.Stack implements Stack03Imp {
}

/**
 * Derive the separate implementations from the common abstraction
 */
class Stack03Mine implements Stack03Imp {
    private Object[] items = new Object[20];
    private int total = -1;

    public Object push(Object o) {
        return items[++total] = o;
    }

    public Object peek() {
        return items[total];
    }

    public Object pop() {
        return items[total--];
    }

    public boolean empty() {
        return total == -1;
    }
}


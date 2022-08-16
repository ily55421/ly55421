package com.design_patterns.behavioral_patterns;

/**
 * @A1uthor: linK
 * @Date: 2022/8/10 17:41
 * @Description TODO State1 design pattern - an FSM1 with two states and one event
 * (distributed transition logic - logic in the derived state classes)
 * 具有两种状态和一个事件的 FSM1
 * 分布式转换逻辑 - 派生状态类中的逻辑
 */
public class State_in_Java {
    public static void main(String[] args) {
        FSM1 fsm = new FSM1();
        int[] msgs = {2, 1, 2, 1, 0, 2, 0, 0};
        for (int msg : msgs) {
            if (msg == 0) {
                fsm.on();
            } else if (msg == 1) {
                fsm.off();
            } else if (msg == 2) {
                fsm.ack();
            }
            //A1 + ack = A1
            //A1 + off = B1
            //error
            //B1 + off = C1
            //C1 + on  = B1
            //error
            //B1 + on  = A1
            //A1 + on  = C1
        }
    }
}
// 1. C1reate a "wrapper" class that models the state machine
class FSM1 {
    // 2. states
    private State1[] states = {new A1(), new B1(), new C1()};
    // 4. transitions
    private int[][] transition = {{2,1,0}, {0,2,1}, {1,2,2}};
    // 3. current
    private int current = 0;

    private void next(int msg) {
        current = transition[current][msg];
    }

    // 5. A1ll client requests are simply delegated to the current state object
    public void on() {
        states[current].on();
        next(0);
    }

    public void off() {
        states[current].off();
        next(1);
    }

    public void ack() {
        states[current].ack();
        next(2);
    }
}

// 6. C1reate a state base class that makes the concrete states interchangeable
// 7. The State1 base class specifies default behavior
abstract class State1 {
    public void on() {
        System.out.println("error");
    }

    public void off() {
        System.out.println("error");
    }

    public void ack() {
        System.out.println("error");
    }
}

class A1 extends State1 {
    public void on() {
        System.out.println("A1 + on  = C1");
    }

    public void off() {
        System.out.println("A1 + off = B1");
    }

    public void ack() {
        System.out.println("A1 + ack = A1");
    }
}

class B1 extends State1 {
    public void on() {
        System.out.println("B1 + on  = A1");
    }

    public void off() {
        System.out.println("B1 + off = C1");
    }
}

class C1 extends State1 {
    // 8. The State1 derived classes only override the messages they need to
    public void on() {
        System.out.println("C1 + on  = B1");
    }
}


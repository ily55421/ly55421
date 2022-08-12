package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/10 17:32
 * @Description TODO State in Java: Distributed transition logic
 * Create a "wrapper" class that models the state machine
 * The wrapper class contains an array of state concrete objects
 * The wrapper class contains an index to its "current" state
 * Client requests are delegated to the current state and "this" is passed
 * Create a state base class that makes the concrete states interchangeable
 * The State base class specifies default behavior for all messages
 * The State derived classes only override the messages they need to
 * The derived classes "call back to" the wrapper class to change its current
 */
public class State_Distributed_transition_logic {
    public static void main(String[] args) {
        FSM fsm  = new FSM();
        // 状态消息
        int[] msgs = {2, 1, 2, 1, 0, 2, 0, 0};
        for (int msg : msgs) {
            if (msg == 0) {
                fsm.on();
            } else if (msg == 1) {
                fsm.off();
            } else if (msg == 2) {
                fsm.ack();
                //初始调用a
            }
        }
        //A + ack = A   状态执行命令
        //A + off = B
        //error
        //B + off = C
        //C + on  = B
        //error
        //B + on  = A
        //A + on  = C
    }
}
// 1. The "wrapper" 包装器
class FSM {
    // 2. States array  状态数组
    private State[] states  = {new A(), new B(), new C()};

    // 3. Current state 当前状态
    private int currentState = 0;

    // 4. Delegation and pass the this pointer
    public void on()  {
        //当前状态打开
        states[currentState].on(this);
    }

    public void off() {
        //关闭
        states[currentState].off(this);
    }

    public void ack() {
        //确认
        states[currentState].ack(this);
    }

    public void changeState(int index) {
        //更改状态
        currentState = index;
    }
}

// 5. The State base class  基础状态类
abstract class State {
    // 6. Default behavior  默认行为
    public void on(FSM fsm) {
        System.out.println("error");
    }

    public void off(FSM fsm) {
        System.out.println("error");
    }

    public void ack(FSM fsm) {
        System.out.println("error");
    }
}

class A extends State {
    public void on(  FSM fsm ) {
        // 状态变更
        System.out.println("A + on  = C");
        fsm.changeState(2);
    }

    public void off(FSM fsm) {
        // 状态变更
        System.out.println("A + off = B");
        fsm.changeState(1);
    }

    public void ack(FSM fsm) {
        // 状态变更
        System.out.println("A + ack = A");
        fsm.changeState(0);
    }
}

class B extends State {
    public void on(FSM fsm) {
        System.out.println("B + on  = A");
        fsm.changeState(0);
    }

    public void off(FSM fsm) {
        System.out.println("B + off = C");
        fsm.changeState(2);
    }
}

// 7. Only override some messages 只覆盖一些消息
class C extends State {
    // 8. "call back to" the wrapper class “回调”包装类
    public void on(FSM fsm) {
        System.out.println("C + on  = B");
        fsm.changeState(1);
    }
}


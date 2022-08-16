package com.design_patterns.behavioral_patterns;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: linK
 * @Date: 2022/8/11 9:36
 * @Description TODO
 */
public class State_in_Java03 {
    public static void main(String[] args) throws IOException {
        InputStreamReader is = new InputStreamReader(System.in);
        Chain chain = new Chain();
        while (true) {
            System.out.print("Press 'Enter'");
            is.read();
            chain.pull();
            //Connected to the target VM, address: '127.0.0.1:62230', transport: 'socket'
            //Press 'Enter'1
            //   low speed
            //Press 'Enter'   medium speed
            //Press 'Enter'1
            //   high speed      切换到 high状态时 没有覆写父类的pull的func 执行父类方法  切换到off状态
            //Press 'Enter'   turning off
        }
    }
}

abstract class State03 {
    /**
     * 抽象方法调用
     *
     * @param wrapper
     */
    public void pull(Chain wrapper) {
        wrapper.setState03(new Off());
        System.out.println("   turning off");
    }
}

class Chain {
    private State03 current;

    /**
     * 初试状态
     */
    public Chain() {
        current = new Off();
    }

    /**
     * 状态变更
     *
     * @param state
     */
    public void setState03(State03 state) {
        current = state;
    }

    /**
     * 调用当前子类方法
     */
    public void pull() {
        current.pull(this);
    }
}

class Off extends State03 {
    public void pull(Chain wrapper) {
        wrapper.setState03(new Low());
        System.out.println("   low speed");
    }
}

class Low extends State03 {
    public void pull(Chain wrapper) {
        wrapper.setState03(new Medium());
        System.out.println("   medium speed");
    }
}

class Medium extends State03 {
    public void pull(Chain wrapper) {
        wrapper.setState03(new High());
        System.out.println("   high speed");
    }
}

/**
 * 空对象 什么都不做
 */
class High extends State03 {
}

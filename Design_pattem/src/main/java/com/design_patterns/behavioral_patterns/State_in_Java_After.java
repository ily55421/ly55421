package com.design_patterns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: linK
 * @Date: 2022/8/11 9:59
 * @Description TODO The CeilingFanPullChain02 class is now a wrapper that delegates to its m_current_state reference.
 * Each clause from the "before" case statement is now captured in a State04 derived class.
 * <p>
 * For this simple domain, the State04 pattern is probably over-kill.
 */
public class State_in_Java_After {
    public static void main(String[] args) {
        CeilingFanPullChain02 chain = new CeilingFanPullChain02();
        while (true) {
            System.out.print("Press ENTER");
            getLine();
            chain.pull();
            //Press ENTER1
            //low speed
            //Press ENTER1
            //medium speed
            //Press ENTER1
            //high speed
            //Press ENTER1
            //turning off
            //Press ENTER
        }
    }

    /**
     * 获取输入信息
     *
     * @return
     */
    static String getLine() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        try {
            line = in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return line;
    }
}

/**
 * 状态方法接口
 */
interface State04 {
    void pull(CeilingFanPullChain02 wrapper);
}

/**
 * 吊扇回调链    状态 wrapper
 */
class CeilingFanPullChain02 {
    private State04 currentState04;

    public CeilingFanPullChain02() {
        currentState04 = new Off02();
    }

    public void set_state(State04 s) {
        currentState04 = s;
    }

    public void pull() {
        currentState04.pull(this);
    }
}

class Off02 implements State04 {
    public void pull(CeilingFanPullChain02 wrapper) {
        wrapper.set_state(new Low02());
        System.out.println("low speed");
    }
}

class Low02 implements State04 {
    public void pull(CeilingFanPullChain02 wrapper) {
        wrapper.set_state(new Medium02());
        System.out.println("medium speed");
    }
}

class Medium02 implements State04 {
    public void pull(CeilingFanPullChain02 wrapper) {
        wrapper.set_state(new High02());
        System.out.println("high speed");
    }
}

class High02 implements State04 {
    public void pull(CeilingFanPullChain02 wrapper) {
        wrapper.set_state(new Off02());
        System.out.println("turning off");
    }
}



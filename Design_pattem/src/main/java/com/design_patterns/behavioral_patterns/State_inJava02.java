package com.design_patterns;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: linK
 * @Date: 2022/8/11 9:27
 * @Description TODO State design pattern - an FSM with two states and one event
 * (distributed transition logic - logic in the derived state classes)
 * Create a "wrapper" class that models the state machine
 * The wrapper class maintains a "current" state object
 * All client requests are simply delegated to the current state object and the wrapper object's "this" pointer is passed
 * Create a state base class that makes the concrete states interchangeable
 * The State base class specifies any useful "default" behavior
 * The State derived classes only override the messages they need to o/r
 * The State methods will change the "current" state in the "wrapper"
 */
public class State_inJava02 {
    public static void main(String[] args) throws IOException {
        InputStreamReader is = new InputStreamReader( System.in );
        Button btn = new Button();
        while (true) {
            System.out.print("Press 'Enter'");
            is.read();
            btn.push();
            // Press 'Enter'1
            //   turning ON02     先执行子类off的push方法
            //Press 'Enter'   turning OFF02  在执行 state的push方法
        }
    }
}
// 1. The "wrapper" class
class Button {
    // 2. The "current" state object
    private State02 current;

    public Button() {
        current = OFF02.instance();
    }

    public void setCurrent(State02 s) {
        current = s;
    }

    // 3. The "wrapper" always delegates to the "wrappee"
    public void push() {
        current.push(this);
    }
}

// 4. The "wrappee" hierarchy
class State02 {
    // 5. Default behavior can go in the base class
    public void push(Button b) {
        b.setCurrent(OFF02.instance());
        System.out.println("   turning OFF02");
    }
}

class ON02 extends State02 {
    private static ON02 instance = new ON02();

    private ON02() {}

    public static State02 instance() {
        return instance;
    }
}

class OFF02 extends State02 {
    private static OFF02 instance = new OFF02();
    private OFF02() { }

    public static State02 instance() {
        return instance;
    }
    // 6. Override only the necessary methods
    public void push(Button b) {
        // 7. The "wrappee" may callback to the "wrapper"
        b.setCurrent(ON02.instance());
        System.out.println("   turning ON02");
    }
}


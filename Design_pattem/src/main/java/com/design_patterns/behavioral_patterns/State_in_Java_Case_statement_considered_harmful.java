package com.design_patterns;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: linK
 * @Date: 2022/8/11 9:44
 * @Description TODO  Java 中的状态：案例陈述被认为是有害的
 * Create a State class hierarchy
 * The State base class has a single method void pull(Chain04 wrapper) The method can be abstract, or, it can contain default behavior
 * Create a State derived class for each condition in the if-then-else statement
 * Move each println() statement to the pull() method of its appropriate class
 * Make the Chain04 class a "wrapper" class that models the state machine abstraction
 * The Chain04 class maintains a "current" State object
 * All client requests [i.e. pull()] are simply delegated to the current state object and the wrapper object's "this" pointer is passed
 * The Chain04 class needs a constructor that initializes its current state
 * It also needs a setState() method that the State derived classes can use to change the state of the state machine
 * Call the setState() method as necessary in each of the State derived classes pull() methods
 */
public class State_in_Java_Case_statement_considered_harmful {
    public static void main( String[] args ) throws IOException {
        InputStreamReader is = new InputStreamReader( System.in );
        Chain04 chain = new Chain04();
        while (true) {
            System.out.print( "Press 'Enter'" );
            is.read();
            chain.pull();
            //Press 'Enter'1      每次改变 直接操作属性
            //   low speed
            //Press 'Enter'   medium speed
            //Press 'Enter'1
            //   high speed
            //Press 'Enter'   turning off
        }
    }
}
class Chain04 {
    private int state;

    public Chain04() {
        state = 0;
    }

    public void pull() {
        if (state == 0) {
            state = 1;
            System.out.println( "   low speed" );
        } else if (state == 1) {
            state = 2;
            System.out.println( "   medium speed" );
        } else if (state == 2) {
            state = 3;
            System.out.println( "   high speed" );
        } else {
            state = 0;
            System.out.println( "   turning off" );
        }
    }
}


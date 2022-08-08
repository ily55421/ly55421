package com.design.statePattern;

/**
 * @author lin 2022/8/8 21:58
 */
public class StartState implements State {

    public void doAction(Context context) {
        System.out.println("Player is in start state");
        context.setState(this);
    }

    public String toString(){
        return "Start State";
    }
}
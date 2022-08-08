package com.design.statePattern;

/**
 * @author lin 2022/8/8 21:58
 */
public class Context {
    private State state;

    public Context(){
        state = null;
    }

    public void setState(State state){
        this.state = state;
    }

    public State getState(){
        return state;
    }
}
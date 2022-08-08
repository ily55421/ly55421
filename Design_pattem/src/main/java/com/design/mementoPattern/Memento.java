package com.design.mementoPattern;

/**
 * @author lin 2022/8/8 21:00
 * TODO 备忘
 */
public class Memento {
    private String state;

    public Memento(String state){
        this.state = state;
    }

    public String getState(){
        return state;
    }
}

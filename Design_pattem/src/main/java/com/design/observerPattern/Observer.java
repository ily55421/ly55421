package com.design.observerPattern;

/**
 * @author lin 2022/8/8 21:53
 */
public abstract class Observer {
    protected Subject subject;
    public abstract void update();
}

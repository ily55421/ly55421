package com.design.observerPattern;


import java.util.ArrayList;
import java.util.List;

/**
 * @author lin 2022/8/8 21:52
 */
public class Subject {

    private List<Observer> observers
            = new ArrayList<Observer>();
    private int state;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        //触发唤醒
        notifyAllObservers();
    }

    public void attach(Observer observer) {
        observers.add(observer);
    }

    public void notifyAllObservers() {
        //通知所有被观察者
        for (Observer observer : observers) {
            observer.update();
        }
    }
}
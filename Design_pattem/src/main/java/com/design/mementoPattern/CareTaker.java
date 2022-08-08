package com.design.mementoPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lin 2022/8/8 21:01
 * TODO 看护人
 */
public class CareTaker {
    private List<Memento> mementoList = new ArrayList<Memento>();

    public void add(Memento state){
        mementoList.add(state);
    }

    public Memento get(int index){
        return mementoList.get(index);
    }
}

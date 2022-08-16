package com.design_patterns.behavioral_patterns;

import java.util.ArrayList;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:45
 * @Description TODO
 */
public class MementoDemo {
    public static void main(String[] args) {
        Caretaker caretaker = new Caretaker();
        Originator originator = new Originator();
        originator.setState("State1");
        originator.setState("State2");
        caretaker.addMemento(originator.save());
        originator.setState("State3");
        caretaker.addMemento(originator.save());
        originator.setState("State4");
        // 恢复上一次状态
        originator.restore(caretaker.getMemento());
        //Originator: Setting state to State1
        //Originator: Setting state to State2
        //Originator: Saving to Memento.
        //Originator: Setting state to State3
        //Originator: Saving to Memento.
        //Originator: Setting state to State4
        //Originator: State after restoring from Memento: State3
    }
}

class Memento {
    private String state;

    public Memento(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}

class Originator {
    private String state;
    /* lots of memory consumptive private data that is not necessary to define the
     * state and should thus not be saved. Hence the small memento object.
     * 大量内存消耗的私有数据，这些数据不是定义状态所必需的，因此不应保存。因此，小纪念品对象。 */
    public void setState(String state) {
        System.out.println("Originator: Setting state to " + state);
        this.state = state;
    }

    public Memento save() {
        System.out.println("Originator: Saving to Memento.");
        return new Memento(state);
    }

    public void restore(Memento m) {
        state = m.getState();
        System.out.println("Originator: State after restoring from Memento: " + state);
    }
}

/**
 * 看守者
 */
class Caretaker {
    private ArrayList<Memento> mementos = new ArrayList<>();

    public void addMemento(Memento m) {
        mementos.add(m);
    }

    public Memento getMemento() {
        return mementos.get(1);
    }
}


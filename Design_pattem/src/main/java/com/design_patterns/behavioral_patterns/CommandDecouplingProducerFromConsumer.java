package com.design_patterns.behavioral_patterns;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/10 13:36
 * @Description TODO
 */
public class CommandDecouplingProducerFromConsumer {
    /**
     * 组合命令
     *
     * @return
     */
    public static List produceRequests() {
        List<Command2> queue = new ArrayList<>();
        queue.add(new DomesticEngineer());
        queue.add(new Politician());
        queue.add(new Programmer());
        return queue;
    }

    /**
     * 消费命令
     *
     * @param queue
     */
    public static void workOffRequests(List queue) {
        for (Object command : queue) {
            ((Command2) command).execute();
        }
    }

    public static void main(String[] args) {
        /**
         * 总体执行
         */
        List queue = produceRequests();
        workOffRequests(queue);
        //take out the trash
        //take money from the rich, take votes from the poor
        //sell the bugs, charge extra for the fixes
    }
}

interface Command2 {
    void execute();
}

class DomesticEngineer implements Command2 {
    public void execute() {
        System.out.println("take out the trash");
    }
}

class Politician implements Command2 {
    public void execute() {
        System.out.println("take money from the rich, take votes from the poor");
    }
}

class Programmer implements Command2 {
    public void execute() {
        System.out.println("sell the bugs, charge extra for the fixes");
    }
}


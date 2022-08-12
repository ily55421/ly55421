package com.design_patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:40
 * @Description TODO
 */
public class MediatorDemo {
    public static void main( String[] args ) {
        List<Thread> producerList = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press ENTER for exit");
        Mediator mb = new Mediator();
        producerList.add(new Thread(new Producer2(mb)));
        producerList.add(new Thread(new Producer2(mb)));
        producerList.add(new Thread(new Consumer2(mb)));
        producerList.add(new Thread(new Consumer2(mb)));
        producerList.add(new Thread(new Consumer2(mb)));
        producerList.add(new Thread(new Consumer2(mb)));
        for (Thread p : producerList) {
            p.start();
        }
        boolean stop = false;
        String exit = scanner.nextLine();
        while (!stop) {
            if (exit.equals("")) {
                stop = true;
                for (Thread p : producerList) {
                    //noinspection deprecation
                    p.stop();
                }
            }
        }
    }
}
// 1. The "intermediary" 中介
class Mediator {
    // 4. The Mediator arbitrates
    private boolean slotFull = false;
    private int number;

    /**
     * 消息商店
     * @param num
     */
    public synchronized void storeMessage(int num) {
        // no room for another message
        while (slotFull == true) {
            try {
                wait();
            }
            catch (InterruptedException e ) {
                // 线程中断
                Thread.currentThread().interrupt();
            }
        }
        slotFull = true;
        number = num;
        notifyAll();
    }

    /**
     * 检索消息
     * @return
     */
    public synchronized int retrieveMessage() {
        // no message to retrieve
        while (slotFull == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        slotFull = false;
        notifyAll();
        return number;
    }
}

/**
 * 
 */
class Producer2 implements Runnable {
    // 2. Producer2s are coupled only to the Mediator
    private Mediator med;
    private int id;
    private static int num = 1;

    public Producer2(Mediator m) {
        med = m;
        id = num++;
    }

    @Override
    public void run() {
        int num;
        while (true) {
            med.storeMessage(num = (int)(Math.random()*100));
            System.out.print( "p" + id + "-" + num + "  " );
        }
    }
}
class Consumer2 implements Runnable {
    // 3. Consumer2s are coupled only to the Mediator
    private Mediator med;
    private int    id;
    private static int num = 1;

    public Consumer2(Mediator m) {
        med = m;
        id = num++;
    }

    @Override
    public void run() {
        while (true) {
            System.out.print("c" + id + "-" + med.retrieveMessage() + "  ");
        }
    }
}



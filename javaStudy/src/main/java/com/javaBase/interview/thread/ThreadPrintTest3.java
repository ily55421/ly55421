package com.javaBase.interview.thread;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lin 2022/8/19 0:12   一道面试题
 * <p>
 * A线程打印A
 * B线程打印B
 * C线程打印C
 * <p>
 * 交替打印ABCABC ... 打印100个字符.
 */
public class ThreadPrintTest3 {
    /**
     * 0-A 1-B 2-C
     */
    private static Integer count = 0;

    /**
     * 方式三  条件唤醒线程   封装方法 使用类似链表的方式处理线程请求
     * {@link ReentrantLock.newCondition}
     */
    private static ReentrantLock reentrantLock = new ReentrantLock();

    public  static void main(String[] args)  {
        Condition conditionA = reentrantLock.newCondition();
        Condition conditionB = reentrantLock.newCondition();
        Condition conditionC = reentrantLock.newCondition();
        Thread t1 = new Thread(new PrintRunner(reentrantLock,conditionA,conditionB,34,'A'));
        Thread t2 = new Thread(new PrintRunner(reentrantLock,conditionB,conditionC,33,'B'));
        Thread t3 = new Thread(new PrintRunner(reentrantLock,conditionC,conditionA,33,'C'));
        t1.start();
        t2.start();
        t3.start();

    }

    /**
     * 定义打印方法
     */

    static class PrintRunner implements Runnable {

        private ReentrantLock reentrantLock;
        private  Condition curCondition;
        private Condition nextCondition;
        private Integer count;
        private Character character;
        private Integer index = 0;


        public PrintRunner(ReentrantLock reentrantLock, Condition curCondition, Condition nextCondition, Integer count, Character character) {
            this.reentrantLock = reentrantLock;
            this.curCondition = curCondition;
            this.nextCondition = nextCondition;
            this.count = count;
            this.character = character;
            this.index = index;
        }

        @Override
        public void run() {
            while (true) {

                reentrantLock.lock();
                if (index<=count) {
                    System.out.println(character);
                    index++;
                }
                //唤醒下一个线程
                nextCondition.signal();
                try {
                    curCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                reentrantLock.unlock();
            }

        }
    }

}

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
public class ThreadPrintTest2 {
    /**
     * 0-A 1-B 2-C
     */
    private static Integer count = 0;

    /**
     * 方式二  条件唤醒线程
     * {@link ReentrantLock.newCondition}
     */
    private static ReentrantLock reentrantLock = new ReentrantLock();

    public  static void main(String[] args)  {
        Condition conditionA = reentrantLock.newCondition();
        Condition conditionB = reentrantLock.newCondition();
        Condition conditionC = reentrantLock.newCondition();
        Thread t1 = new Thread(() -> {
            while (true) {
                reentrantLock.lock();
                    if (count + 1 <= 100 ) {
                        System.out.println("A");
                        count++;
                        //启用
                        conditionB.signal();
                        try {
                            conditionA.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }finally {
                            reentrantLock.unlock();
                        }

                    }
            }
        });
        Thread t2 = new Thread(() -> {
            while (true) {
                reentrantLock.lock();
                if (count + 1 <= 100 ) {
                    System.out.println("B");
                    count++;
                    //启用
                    conditionC.signal();
                    try {
                        conditionB.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        reentrantLock.unlock();
                    }

                }
            }
        });
        Thread t3 = new Thread(() -> {
            while (true) {
                reentrantLock.lock();
                if (count + 1 <= 100 ) {
                    System.out.println("C");
                    count++;
                    //启用
                    conditionA.signal();
                    try {
                        conditionC.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        reentrantLock.unlock();
                    }

                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
    }


}

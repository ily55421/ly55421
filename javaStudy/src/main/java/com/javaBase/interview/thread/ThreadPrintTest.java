package com.javaBase.interview.thread;

/**
 * @author lin 2022/8/19 0:12   一道面试题
 * <p>
 * A线程打印A
 * B线程打印B
 * C线程打印C
 * <p>
 * 交替打印ABCABC ... 打印100个字符.
 */
public class ThreadPrintTest {
    /**
     * 0-A 1-B 2-C
     */
    private static Integer state = 0;
    private static Object lock = new Object();
    private static Integer count = 0;
    public static   void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    if (count + 1 <= 100 && state == 0) {
                        System.out.println("A");
                        lock.notify();
                        count++;
                        state = 1;
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        Thread t2 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    if (count + 1 <= 100 && state == 1) {
                        System.out.println("B");
                        lock.notify();
                        count++;
                        state = 2;
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        Thread t3 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    if (count + 1 <= 100 && state == 2) {
                        System.out.println("C");
                        lock.notify();
                        count++;
                        state = 0;
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
    }



}

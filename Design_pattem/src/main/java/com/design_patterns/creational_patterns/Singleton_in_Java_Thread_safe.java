package com.design_patterns.creational_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 17:37
 * @Description TODO  线程安全单例 双重检查锁
 */
public class Singleton_in_Java_Thread_safe {
    private static volatile Singleton_in_Java_Thread_safe instance;

    private Singleton_in_Java_Thread_safe() {}

    public static Singleton_in_Java_Thread_safe getInstance(String value) {
        if (instance == null) {
            synchronized (Singleton_in_Java_Thread_safe.class) {
                if (instance == null) {
                    instance = new Singleton_in_Java_Thread_safe();
                }
            }
        }
        return instance;
    }
}

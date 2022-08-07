package com.design.singletonPattern;

/**
 * 懒汉式，线程不安全
 * 1、是否 Lazy 初始化： 是
 * 2、是否多线程安全： 否
 * 3、实现难度： 易
 * 4、描述： 这种方式是最基本的实现方式，这种实现最大的问题就是不支持多线程
 */
public class SingletonLazyUnSafe {
    private static SingletonLazyUnSafe instance;

    private SingletonLazyUnSafe() {
    }

    public static SingletonLazyUnSafe getInstance() {
        if (instance == null) {
            // 实用程序类“SingletonLazyUnSafe”的实例化
            instance = new SingletonLazyUnSafe();
        }
        return instance;
    }
}

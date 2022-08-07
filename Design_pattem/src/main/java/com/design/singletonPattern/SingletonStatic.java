package com.design.singletonPattern;

/**
 * 5. 登记式/静态内部类
 * 1、是否 Lazy 初始化： 是
 * 2、是否多线程安全： 是
 * 3、实现难度： 一般
 * 4、描述： 这种方式能达到双检锁方式一样的功效，但实现更简单
 */
public class SingletonStatic {
    private static class SingletonHolder {
        private static final SingletonStatic INSTANCE = new SingletonStatic();
    }

    private SingletonStatic() {
    }

    public static final SingletonStatic getInstance() {
        return SingletonHolder.INSTANCE;
    }

}

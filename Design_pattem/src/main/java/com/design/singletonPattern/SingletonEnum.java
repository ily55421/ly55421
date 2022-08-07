package com.design.singletonPattern;

/**
 * 6. 枚举
 * 1、JDK 版本： JDK1.5 起
 * 2、是否 Lazy 初始化： 否
 * 3、是否多线程安全： 是
 * 4、实现难度： 易
 * 5、描述：  TODO 这种实现方式还没有被广泛采用，但这是实现单例模式的最佳方法
 */
public enum SingletonEnum {
    INSTANCE;
    public void whateverMethod() {
    }
}
package com.design.singletonPattern;

/**
 * 4. 双检锁/双重校验锁（DCL，即 double-checked locking）
 * 1、JDK 版本： JDK1.5 起
 * 2、是否 Lazy 初始化： 是
 * 3、是否多线程安全： 是
 * 4、实现难度： 较复杂
 * 5、描述： 这种方式采用双锁机制，安全且在多线程情况下能保持高性能。getInstance() 的性能对应用程序很关键
 */
public class SingletonDoubleCheckedLocking {
    private volatile static SingletonDoubleCheckedLocking singleton;
    private SingletonDoubleCheckedLocking (){}
    public static SingletonDoubleCheckedLocking getSingleton() {
        if (singleton == null) {
            synchronized (SingletonDoubleCheckedLocking.class) {
                if (singleton == null) {
                    singleton = new SingletonDoubleCheckedLocking();
                }
            }
        }
        return singleton;
    }
}

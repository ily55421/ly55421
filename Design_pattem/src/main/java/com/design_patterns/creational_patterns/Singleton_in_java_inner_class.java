package com.design_patterns.creational_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 17:15
 * @Description TODO  通过内部类的方式创建单例
 * The inner class is referenced no earlier (and therefore loaded no earlier by the class loader)
 * than the moment that getInstance() is called.
 * Thus, this solution is thread-safe without requiring special language constructs (i.e. volatile or synchronized).
 * 内部类的引用不早于调用 getInstance() 的那一刻（因此类加载器不早于加载）。因此，该解决方案是线程安全的，
 * 不需要特殊的语言结构（即 volatile 或 synchronized）。
 */
public class Singleton_in_java_inner_class {
    private Singleton_in_java_inner_class() {
    }

    /**
     * 单例持有者
     */
    private static class SingletonHolder {
        private static final Singleton_in_java_inner_class INSTANCE = new Singleton_in_java_inner_class();
    }

    public static Singleton_in_java_inner_class getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

package com.design.singletonPattern;

/**
 * 2. 懒汉式，线程安全
 * 1、是否 Lazy 初始化： 是
 * 2、是否多线程安全： 是
 * 3、实现难度： 易
 * 4、描述：
 */
public class SingletonLazySafe {
        private static SingletonLazySafe instance;
        private SingletonLazySafe (){}
        public static synchronized SingletonLazySafe getInstance() {
            if (instance == null) {
                instance = new SingletonLazySafe();
            }
            return instance;
        }
}

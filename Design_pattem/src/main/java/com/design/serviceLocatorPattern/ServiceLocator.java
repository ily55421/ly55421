package com.design.serviceLocatorPattern;

/**
 * @author lin 2022/8/9 1:10
 */

public class ServiceLocator {
    private static Cache cache;

    static {
        cache = new Cache();
    }

    public static Service getService(String jndiName){
        //缓存存储
        Service service = cache.getService(jndiName);

        //缓存
        if(service != null){
            return service;
        }

        InitialContext context = new InitialContext();
        //初始化上下文 获取实例对象
        Service service1 = (Service)context.lookup(jndiName);
        cache.addService(service1);
        return service1;
    }
}
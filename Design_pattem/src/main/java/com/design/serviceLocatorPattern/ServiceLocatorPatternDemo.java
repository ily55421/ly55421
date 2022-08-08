package com.design.serviceLocatorPattern;

/**
 * @author lin 2022/8/9 1:10
 */
public class ServiceLocatorPatternDemo
{
    public static void main(String[] args) {
        Service service = ServiceLocator.getService("Service1");
        service.execute();
        service = ServiceLocator.getService("Service2");
        service.execute();
        service = ServiceLocator.getService("Service1");
        service.execute();
        service = ServiceLocator.getService("Service2");
        service.execute();
        //Looking up and creating a new Service1 object
        //Executing Service1
        //Looking up and creating a new Service2 object
        //Executing Service2
        //Returning cached  Service1 object
        //Executing Service1
        //Returning cached  Service2 object
        //Executing Service2
    }
}

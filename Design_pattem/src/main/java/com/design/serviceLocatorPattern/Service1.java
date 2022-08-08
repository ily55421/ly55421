package com.design.serviceLocatorPattern;

/**
 * @author lin 2022/8/9 1:09
 */
public class Service1 implements Service {
    public void execute(){
        System.out.println("Executing Service1");
    }

    @Override
    public String getName() {
        return "Service1";
    }
}

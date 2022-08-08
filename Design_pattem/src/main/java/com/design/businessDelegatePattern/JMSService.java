package com.design.businessDelegatePattern;

/**
 * @author lin 2022/8/8 22:29
 */


public class JMSService implements BusinessService {

    @Override
    public void doProcessing() {
        System.out.println("Processing task by invoking JMS Service");
    }
}

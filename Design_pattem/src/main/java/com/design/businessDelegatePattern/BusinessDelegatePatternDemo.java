package com.design.businessDelegatePattern;

/**
 * @author lin 2022/8/8 22:31
 */
public class BusinessDelegatePatternDemo {

    public static void main(String[] args) {

        BusinessDelegate businessDelegate = new BusinessDelegate();
        businessDelegate.setServiceType("EJB");

        Client client = new Client(businessDelegate);
        client.doTask();

        businessDelegate.setServiceType("JMS");
        client.doTask();
        //Processing task by invoking EJB Service
        //Processing task by invoking JMS Service
    }
}

package com.design.businessDelegatePattern;

/**
 * @author lin 2022/8/8 22:30
 */

public class Client {

    BusinessDelegate businessService;

    public Client(BusinessDelegate businessService){
        this.businessService  = businessService;
    }

    public void doTask(){
        businessService.doTask();
    }
}

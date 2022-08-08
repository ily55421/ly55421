package com.design.nullObjectPattern;

/**
 * @author lin 2022/8/8 22:03
 */
public class NullPatternDemo {
    public static void main(String[] args) {
        // 不存在的内容 进行null对象创建
        AbstractCustomer customer1 = CustomerFactory.getCustomer("Rob");
        AbstractCustomer customer2 = CustomerFactory.getCustomer("Bob");
        AbstractCustomer customer3 = CustomerFactory.getCustomer("Julie");
        AbstractCustomer customer4 = CustomerFactory.getCustomer("Laura");

        System.out.println("Customers");
        System.out.println(customer1.getName());
        System.out.println(customer2.getName());
        System.out.println(customer3.getName());
        System.out.println(customer4.getName());
        //Customers
        //Rob
        //Not Available in Customer Database
        //Julie
        //Not Available in Customer Database
    }
}

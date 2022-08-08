package com.design.compositeEntityPattern;

/**
 * @author lin 2022/8/9 0:37
 */
public class CompositeEntityPatternDemo {
    public static void main(String[] args) {
        Client client = new Client();
        // 组合实体进行赋值
        client.setData("Test", "Data");
        client.printData();
        client.setData("Second Test", "Data1");
        client.printData();
    }
    //Data: Test
    //Data: Data
    //Data: Second Test
    //Data: Data1
}

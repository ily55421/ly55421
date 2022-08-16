package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:08
 * @Description TODO 用显式方法替换参数
 */
public class Replace_Parameter_with_Explicit_Methods {
    /*
    问题
一个方法被分成几个部分，每个部分都根据参数的值运行。

解决方案
将方法的各个部分提取到它们自己的方法中并调用它们而不是原始方法。

为什么要重构
一种包含参数相关变体的方法已经变得庞大。每个分支都运行重要的代码，并且很少添加新的变体。

好处
提高代码可读性。比 setValue("engineEnabled", true) 更容易理解 startEngine() 的目的。

何时不使用
TODO 如果方法很少更改并且未在其中添加新变体，则不要用显式方法替换参数。

如何重构
对于方法的每个变体，创建一个单独的方法。根据 main 方法中的参数值运行这些方法。
查找调用原始方法的所有位置。在这些地方，调用新的参数相关变体之一。
当没有对原始方法的调用仍然存在时，将其删除。

     */
}
class Replace_Parameter_with_Explicit_Methods_Before{
    private int width;
    private int height;

    void setValue(String name, int value) {
        if (name.equals("height")) {
            height = value;
            return;
        }
        if (name.equals("width")) {
            width = value;
            return;
        }
//        Assert.shouldNeverReachHere();
    }
}
class Replace_Parameter_with_Explicit_Methods_After{
    private int width;
    private int height;

    void setHeight(int arg) {
        height = arg;
    }
    void setWidth(int arg) {
        width = arg;
    }

}
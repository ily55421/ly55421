package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 11:25
 * @Description TODO 创建方法对象并调用其main方法来替换原始类中原始方法的主体
 */
public class Replace_Method_with_Method_Object {
    /*
问题
您有一个很长的方法，其中局部变量交织在一起，以至于您无法应用提取方法。

解决方案
将方法转换为单独的类，使局部变量成为类的字段。然后，您可以将该方法拆分为同一类中的多个方法。


    为什么要重构
一个方法太长了，由于大量的局部变量难以相互隔离，因此您无法将其分开。
第一步是将整个方法隔离到一个单独的类中，并将其局部变量变成该类的字段。
首先，这允许在类级别隔离问题。其次，它为将大而笨重的方法拆分成更小的方法铺平了道路，这些小方法无论如何都不符合原始类的目的。

好处
在自己的类中隔离一个长方法可以阻止方法的大小膨胀。这也允许将其拆分为类中的子方法，而不会使用实用方法污染原始类。

缺点
添加了另一个类，增加了程序的整体复杂性。

如何重构
创建一个新类。根据您正在重构的方法的目的命名它。
在新类中，创建一个私有字段，用于存储对该方法先前所在类的实例的引用。如果需要，它可以用于从原始类中获取一些所需的数据。
为方法的每个局部变量创建一个单独的私有字段。
创建一个构造函数，该构造函数接受该方法的所有局部变量的值作为参数，并初始化相应的私有字段。
声明主方法并将原始方法的代码复制到其中，将局部变量替换为私有字段。
通过创建方法对象并调用其main方法来替换原始类中原始方法的主体。

     */
}

class Replace_Method_with_Method_Object_Before {
    // ...
    public double price() {
        double primaryBasePrice;
        double secondaryBasePrice;
        double tertiaryBasePrice;
        // Perform long computation.
        return 0.0;
    }
}

/**
 * 将内部长方法  抽取到 一个方法对象中
 */
class Replace_Method_with_Method_Object_After {
    // ...
    public double price() {
        return new PriceCalculator(this).compute();
    }
}

class PriceCalculator {
    private double primaryBasePrice;
    private double secondaryBasePrice;
    private double tertiaryBasePrice;

    public PriceCalculator(Replace_Method_with_Method_Object_After order) {
        // Copy relevant information from the
        // order object.
    }

    public double compute() {
        // Perform long computation.
        return 0.0;
    }
}
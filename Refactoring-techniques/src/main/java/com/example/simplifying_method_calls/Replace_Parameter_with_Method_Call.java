package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:31
 * @Description TODO 用方法调用替换参数 (不要通过参数传递值，而是将获取值的代码放在方法中。)
 */
public class Replace_Parameter_with_Method_Call {
    /*

    问题
在方法调用之前，运行第二个方法并将其结果作为参数发送回第一个方法。但是参数值可以在被调用的方法内部获得。

解决方案
不要通过参数传递值，而是将获取值的代码放在方法中。

为什么要重构
一长串参数很难理解。此外，对此类方法的调用通常类似于一系列级联，具有难以导航但必须传递给方法的曲折和令人振奋的值计算。
因此，如果可以借助方法计算参数值，请在方法本身内部执行此操作并去掉参数。

好处
我们摆脱了不需要的参数并简化了方法调用。这些参数通常不是像现在那样为项目创建，而是着眼于可能永远不会出现的未来需求。

缺点
您明天可能需要该参数来满足其他需求……让您重写该方法。

如何重构
确保获取值的代码不使用当前方法中的参数，因为它们在另一个方法中不可用。如果是这样，移动代码是不可能的。
如果相关代码比单个方法或函数调用更复杂，请使用提取方法将这段代码隔离在一个新的方法中，并使调用变得简单。
在 main 方法的代码中，将所有对被替换参数的引用替换为对获取值的方法的调用。
使用 Remove Parameter 消除现在未使用的参数。


     */
}
class Replace_Parameter_with_Method_Call_Before{
    private int quantity;
    private int itemPrice;

    int basePrice = quantity * itemPrice;
    double seasonDiscount = this.getSeasonalDiscount();
    double fees = this.getFees();
    double finalPrice = discountedPrice(basePrice, seasonDiscount, fees);



    private double discountedPrice(int basePrice, double seasonDiscount, double fees) {
        return 0;
    }

    double getSeasonalDiscount() {
        return 0;
    }

    double getFees() {
        return 0;
    }
}

class Replace_Parameter_with_Method_Call_After{
    private int quantity;
    private int itemPrice;

    int basePrice = quantity * itemPrice;
    double finalPrice = discountedPrice(basePrice);

    private double discountedPrice(int basePrice) {
        return 0;
    }

}


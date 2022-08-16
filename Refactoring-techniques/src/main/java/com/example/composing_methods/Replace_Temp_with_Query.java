package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:48
 * @Description TODO 将变量替换为对新方法的查询
 */
public class Replace_Temp_with_Query {
    /*
    问题
您将表达式的结果放在局部变量中，以供以后在代码中使用。

解决方案
将整个表达式移动到一个单独的方法并从中返回结果。查询方法而不是使用变量。如有必要，将新方法合并到其他方法中。

为什么要重构
这种重构可以为将提取方法应用于很长方法的一部分奠定基础。
有时也可以在其他方法中找到相同的表达式，这是考虑创建通用方法的原因之一。

好处
代码可读性。 getTax() 方法的目的比 orderPrice() * 0.2 更容易理解。
如果要替换的行在多种方法中使用，则通过重复数据删除来精简代码。

很高兴知道
表现
这种重构可能会引发这样一个问题，即这种方法是否容易导致性能下降。诚实的答案是：是的，因为结果代码可能会因查询新方法而负担过重。
但是使用当今快速的 CPU 和出色的编译器，负担几乎总是最小的。
相比之下，由于这种重构方法，可读代码和在程序代码的其他地方重用此方法的能力是非常明显的好处。
尽管如此，如果您的临时变量用于缓存真正耗时的表达式的结果，您可能希望在将表达式提取到新方法后停止此重构。

如何重构
确保在方法中将值分配给变量一次且仅一次。
如果不是，请使用拆分临时变量以确保该变量仅用于存储表达式的结果。
使用提取方法将感兴趣的表达置于新方法中。
确保此方法仅返回一个值，并且不会更改对象的状态。如果该方法影响对象的可见状态，请使用从修饰符中分离查询。
将变量替换为对新方法的查询。



     */
}
class Replace_Temp_with_Query_Before{
    private double quantity = 0.0;
    private double itemPrice = 0.0;
    double calculateTotal() {
        double basePrice = quantity * itemPrice;
        if (basePrice > 1000) {
            return basePrice * 0.95;
        }
        else {
            return basePrice * 0.98;
        }
    }

}
class Replace_Temp_with_Query_After{
    private double quantity = 0.0;
    private double itemPrice = 0.0;

    double calculateTotal() {
        if (basePrice() > 1000) {
            return basePrice() * 0.95;
        }
        else {
            return basePrice() * 0.98;
        }
    }
    double basePrice() {
        return quantity * itemPrice;
    }
}
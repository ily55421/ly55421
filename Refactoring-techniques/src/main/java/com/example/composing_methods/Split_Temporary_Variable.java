package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:52
 * @Description TODO 区分 拆分 使用该变量值的地方使用新名称而不是旧名称
 */
public class Split_Temporary_Variable {
    /*
问题
您有一个局部变量，用于在方法内存储各种中间值（循环变量除外）。

解决方案
对不同的值使用不同的变量。每个变量应该只负责一件特定的事情。

    为什么要重构
如果您在函数中忽略变量的数量并将它们重用于各种不相关的目的，那么当您需要更改包含变量的代码时，您肯定会遇到问题。
您必须重新检查变量使用的每种情况，以确保使用正确的值。

好处
程序代码的每个组件应该只负责一件事。
这使得代码的维护变得更加容易，因为您可以轻松地替换任何特定的东西，而不必担心意外的影响。
代码变得更具可读性。
如果一个变量是很久以前匆忙创建的，它的名称可能无法解释任何内容：k、a2、值等。
但是您可以通过以易于理解、不言自明的方式命名新变量来解决这种情况。
此类名称可能类似于 customerTaxValue、cityUnemploymentRate、clientSalutationString 等。
如果您预计稍后使用提取方法，则此重构技术很有用。

如何重构
在代码中找到变量被赋值的第一个位置。在这里，您应该使用与分配的值相对应的名称来重命名变量。
在使用该变量值的地方使用新名称而不是旧名称。
根据需要重复为变量分配不同值的地方。

     */
}

class Split_Temporary_Variable_Before {
    private int height;
    private int width;

    public void clan() {
        double temp = 2 * (height + width);
        System.out.println(temp);
        temp = height * width;
        System.out.println(temp);
    }
}

/**
 * 使用明确的表意 存储对应的零食变量
 */
class Split_Temporary_Variable_After {
    private int height;
    private int width;

    public void clan(){
        final double perimeter = 2 * (height + width);
        System.out.println(perimeter);
        final double area = height * width;
        System.out.println(area);
    }
}
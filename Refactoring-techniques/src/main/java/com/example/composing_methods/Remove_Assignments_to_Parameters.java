package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 11:19
 * @Description TODO 删除对参数的分配
 */
public class Remove_Assignments_to_Parameters {

    /*
    问题
一些值被分配给方法体内的参数。

解决方案
使用局部变量而不是参数。

为什么要重构
这种重构的原因与拆分临时变量的原因相同，但在这种情况下，我们处理的是参数，而不是局部变量。
首先，如果通过引用传递参数，则在方法内部更改参数值后，将该值传递给请求调用此方法的参数。
很多时候，这会意外发生并导致不幸的后果。即使在您的编程语言中参数通常是通过值（而不是通过引用）传递的，这种编码怪癖可能会疏远那些不习惯它的人。
其次，将不同值多次分配给单个参数使您很难知道在任何特定时间点参数中应包含哪些数据。
如果您的参数及其内容已记录在案，但实际值可能与方法内部的预期值不同，则问题会变得更糟。

好处
程序的每个元素应该只负责一件事。这使得代码维护变得更加容易，因为您可以安全地替换代码而不会产生任何副作用。
这种重构有助于提取“重复代码以分离方法”（提取方法）。

如何重构
创建一个局部变量并分配参数的初始值。
在此行之后的所有方法代码中，将参数替换为新的局部变量。

     */
}

class Remove_Assignments_to_Parameters_Before{
    int discount(int inputVal, int quantity) {
        if (quantity > 50) {
            inputVal -= 2;
        }
        // ...
        return 0;
    }
}
class Remove_Assignments_to_Parameters_After{
    int discount(int inputVal, int quantity) {
        int result = inputVal;
        if (quantity > 50) {
            result -= 2;
        }
        // ...
        return 0;
    }
}
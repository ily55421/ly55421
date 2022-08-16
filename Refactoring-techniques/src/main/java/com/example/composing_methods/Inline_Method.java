package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:33
 * @Description TODO
 */
public class Inline_Method {
    /*

问题
当方法主体比方法本身更明显时，请使用此技术。

解决方案
用方法的内容替换对方法的调用并删除方法本身。

为什么要重构
一个方法只是委托给另一个方法。这个代表团本身没有问题。但是当有很多这样的方法时，它们就变成了一个难以理清的混乱纠结。
通常方法最初并不太短，但随着程序的更改而变得如此。所以不要羞于摆脱那些已经过时的方法。

好处
通过最小化不需要的方法的数量，您可以使代码更直接。

如何重构
确保没有在子类中重新定义该方法。如果重新定义该方法，请避免使用此技术。
查找对该方法的所有调用。将这些调用替换为方法的内容。
删除方法。



     */
}
class Inline_Method_Before{
    private int numberOfLateDeliveries = 0;
    // ...
    int getRating() {
        return moreThanFiveLateDeliveries() ? 2 : 1;
    }
    boolean moreThanFiveLateDeliveries() {
        return numberOfLateDeliveries > 5;
    }
}

/**
 * 变量直接使用比用 方法抽取更加直接 直观
 */
class Inline_Method_After{
    private int numberOfLateDeliveries = 0;
    // ...
    int getRating() {
        return numberOfLateDeliveries > 5 ? 2 : 1;
    }
}
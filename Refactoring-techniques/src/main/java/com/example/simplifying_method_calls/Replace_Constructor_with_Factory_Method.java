package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:42
 * @Description TODO 创建一个工厂方法并使用它来替换构造函数调用
 */
public class Replace_Constructor_with_Factory_Method {
/*
问题
您有一个复杂的构造函数，它不仅仅在对象字段中设置参数值。

解决方案
创建一个工厂方法并使用它来替换构造函数调用。

为什么要重构
使用这种重构技术的最明显原因与用子类替换类型代码有关。
您的代码中先前创建了一个对象，并将编码类型的值传递给它。使用重构方法后，出现了几个子类，您需要根据编码类型的值从它们中创建对象。更改原始构造函数以使其返回子类对象是不可能的，因此我们创建了一个静态工厂方法，该方法将返回必要类的对象，之后它会替换对原始构造函数的所有调用。
当构造函数不能胜任任务时，工厂方法也可以在其他情况下使用。在尝试将值更改为参考时，它们可能很重要。它们还可用于设置超出参数数量和类型的各种创建模式。

好处
工厂方法不一定返回调用它的类的对象。通常这些可能是它的子类，根据给定方法的参数进行选择。
工厂方法可以有一个更好的名称来描述它返回的内容和方式，例如 Troops::GetCrew(myTank)。
工厂方法可以返回一个已经创建的对象，与构造函数不同，它总是创建一个新实例。

如何重构
创建工厂方法。在其中调用当前构造函数。
将所有构造函数调用替换为对工厂方法的调用。

将构造函数声明为私有。
调查构造函数代码并尝试隔离与构造当前类的对象没有直接关系的代码，将此类代码移动到工厂方法中。


 */
}
class Replace_Constructor_with_Factory_Method_Before{
    private int type;
    Replace_Constructor_with_Factory_Method_Before(int type) {
        this.type = type;
    }
    // ...
}

/**
 * 将构造方法替换为工厂方法 create 创建
 */
class Replace_Constructor_with_Factory_Method_After{
    private int type;
    private static Replace_Constructor_with_Factory_Method_After employee;
    Replace_Constructor_with_Factory_Method_After(int type) {
        this.type = type;
    }

    static Replace_Constructor_with_Factory_Method_After create(int type) {
        employee = new Replace_Constructor_with_Factory_Method_After(type);
        // do some heavy lifting.
        return employee;
    }
    // ...
}
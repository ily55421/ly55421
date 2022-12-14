package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/16 8:43
 * @Description TODO  用委托代替继承
 */
public class Replace_Inheritance_with_Delegation {
/*
问题
你有一个子类，它只使用了它超类的一部分方法（或者不可能继承超类的数据）。
解决方案
创建一个字段并在其中放入一个超类对象，将方法委托给超类对象，并摆脱继承。

为什么要重构
在以下情况下，用组合替换继承可以显着改善类设计：
您的子类违反了 Liskov 替换原则，即，如果实现继承只是为了组合公共代码，而不是因为子类是超类的扩展。
子类只使用了超类的一部分方法。在这种情况下，有人调用他或她不应该调用的超类方法只是时间问题。
本质上，这种重构技术将两个类分开，并使超类成为子类的助手，而不是其父类。子类不会继承所有超类方法，而是只有必要的方法来委托给超类对象的方法。

好处
一个类不包含从超类继承的任何不需要的方法。
可以将具有各种实现的各种对象放在委托字段中。实际上你得到了一个策略设计模式。

缺点
您必须编写许多简单的委托方法。

如何重构
在子类中创建一个字段来保存超类。在初始阶段，将当前对象放入其中。
更改子类方法，以便它们使用超类对象而不是 this。
对于在客户端代码中调用的从超类继承的方法，在子类中创建简单的委托方法。
从子类中删除继承声明。
通过创建新对象来更改存储前一个超类的字段的初始化代码。

 */
}

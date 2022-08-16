package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/16 8:38
 * @Description TODO 为它们创建一个共享超类，并将所有相同的字段和方法移至其中
 */
public class Extract_Superclass {
/*
问题
您有两个具有公共字段和方法的类。
解决方案
为它们创建一个共享超类，并将所有相同的字段和方法移至其中。

为什么要重构
当两个类以相同的方式执行相似的任务，或以不同的方式执行相似的任务时，就会发生一种类型的代码重复。对象提供了一种内置机制，通过继承来简化这种情况。但通常这种相似性在创建类之前不会被注意到，因此需要稍后创建继承结构。

好处
代码重复数据删除。通用字段和方法现在只“存在”在一个地方。

何时不使用
您不能将此技术应用于已经具有超类的类。

如何重构
创建一个抽象超类。
使用上拉字段、上拉方法和上拉构造函数体将通用功能移动到超类。从字段开始，因为除了常用字段之外，您还需要移动常用方法中使用的字段。
在客户端代码中寻找可以用新类替换子类的位置（例如在类型声明中）。
 */
}
package com.example.moving_features_betweeno_objects;

/**
 * @Author: linK
 * @Date: 2022/8/15 13:56
 * @Description TODO 将所有特征从类移到另一个。
 */
public class Inline_Class {
    /*
    问题
一个类几乎什么都不做，也不对任何事情负责，也没有为它计划额外的职责。

解决方案
解决方案：将所有特征从类移到另一个。

为什么要重构
在一个类的特征被“移植”到其他类之后，通常需要这种技术，使该类无事可做。

好处
消除不必要的类可以释放计算机上的操作内存——以及你头脑中的带宽。

如何重构
在接收者类中，创建捐助者类中的公共字段和方法。方法应该参考捐助者类的等效方法。
将所有对捐助者类的引用替换为对接收者类的字段和方法的引用。
现在测试程序并确保没有添加任何错误。如果测试显示一切正常，则开始使用 Move Method 和 Move Field 将所有功能从原始类完全移植到接收者类。
继续这样做，直到原始类完全为空。
删除原来的类。


     */
}

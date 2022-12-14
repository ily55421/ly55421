package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:36
 * @Description TODO 引入参数对象  将这些参数替换为对象
 */
public class Introduce_Parameter_Object {
/*
问题
您的方法包含一组重复的参数。

解决方案
将这些参数替换为对象。

为什么要重构
在多种方法中经常会遇到相同的参数组。这会导致参数本身和相关操作的代码重复。
通过将参数合并到单个类中，您还可以将用于处理此数据的方法也移到那里，从而将其他方法从该代码中解放出来。

好处
更具可读性的代码。您看到的不是大杂烩的参数，而是具有易于理解的名称的单个对象。
散布在各处的相同参数组创建了它们自己的代码重复：虽然没有调用相同的代码，但不断遇到相同的参数和参数组。

缺点
如果您只将数据移动到新类并且不打算将任何行为或相关操作移动到那里，那么这就会开始散发出数据类的味道。

如何重构
创建一个代表您的参数组的新类。使类不可变。
在要重构的方法中，使用 Add Parameter，这是传递参数对象的位置。在所有方法调用中，将从旧方法参数创建的对象传递给此参数。
现在开始从方法中一一删除旧参数，将代码中的旧参数替换为参数对象的字段。每次更换参数后测试程序。
TODO 完成后，看看将方法的一部分（有时甚至是整个方法）移动到参数对象类是否有任何意义。如果是这样，请使用移动方法或提取方法。

 */
}

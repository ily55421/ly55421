package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:18
 * @Description TODO
 */
public class Change_Reference_to_Value {
    /*
    问题
您有一个太小且不经常更改的引用对象，无法证明管理其生命周期是合理的。

解决方案
把它变成一个值对象。

为什么要重构
从引用切换到值的灵感可能来自使用引用的不便。引用需要您进行管理：
他们总是需要从存储中请求必要的对象。
内存中的引用可能不方便使用。
与值相比，在分布式和并行系统上使用引用特别困难。
如果您宁愿拥有不可更改的对象而不是状态可能在其生命周期内发生变化的对象，则值特别有用。

好处
对象的一个​​重要属性是它们应该是不可更改的。每个返回对象值的查询都应该收到相同的结果。如果这是真的，那么如果有许多对象代表同一事物，则不会出现问题。
价值观更容易实现。

缺点
如果值是可更改的，请确保如果任何对象发生更改，则表示同一实体的所有其他对象中的值都会更新。这是非常繁重的，因此为此目的创建参考更容易。

如何重构
使对象不可更改。对象不应该有任何设置器或其他改变其状态和数据的方法（删除设置方法在这里可能会有所帮助）。应该将数据分配给值对象的字段的唯一位置是构造函数。
创建一个比较方法，以便能够比较两个值。
检查是否可以删除工厂方法并使对象构造函数公开。

     */
}
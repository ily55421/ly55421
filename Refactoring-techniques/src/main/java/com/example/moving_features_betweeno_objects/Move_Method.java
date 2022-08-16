package com.example.moving_features_betweeno_objects;

/**
 * @Author: linK
 * @Date: 2022/8/15 13:51
 * @Description TODO
 */
public class Move_Method {
    /*

    问题
一个方法在另一个类中的使用比在它自己的类中更多。

解决方案
在使用该方法最多的类中创建一个新方法，然后将代码从旧方法移到那里。将原始方法的代码转换为对另一个类中新方法的引用，否则将其完全删除。

为什么要重构
您希望将方法移动到包含该方法使用的大部分数据的类。这使得类在内部更加连贯。
您希望移动方法以减少或消除调用该方法的类对其所在类的依赖性。如果调用类已经依赖于您计划将方法移动到的类，这将很有用。这减少了类之间的依赖。

如何重构
验证其类中旧方法使用的所有功能。移动它们也可能是个好主意。
通常，如果某个功能仅由所考虑的方法使用，您当然应该将该功能移至该方法。
如果该功能也被其他方法使用，您也应该移动这些方法。有时移动大量方法比在不同类中建立它们之间的关系要容易得多。
确保方法未在超类和子类中声明。如果是这种情况，您要么必须避免移动，要么在接收者类中实现一种多态性，以确保在捐助者类之间拆分方法的不同功能。
在接收者类中声明新方法。您可能希望为新类中更适合它的方法指定一个新名称。
决定你将如何引用接收者类。您可能已经有一个返回适当对象的字段或方法，但如果没有，您将需要编写一个新方法或字段来存储接收者类的对象。

现在您有一种方法可以引用接收者对象和它的类中的一个新方法。有了所有这些，您就可以将旧方法变成对新方法的引用。

看看：你能完全删除旧方法吗？如果是这样，请在所有使用旧方法的地方引用新方法。

     */
}
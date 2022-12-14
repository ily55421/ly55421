package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/16 8:39
 * @Description TODO 将此相同部分移至其自己的界面  提取接口
 */
public class Extract_Interface {
/*
问题
多个客户端使用类接口的同一部分。另一种情况：两个类中的部分接口是相同的。

解决方案
将此相同部分移至其自己的界面。

为什么要重构
当类在不同情况下扮演特殊角色时，接口非常合适。使用提取接口明确指示哪个角色。
当您需要描述一个类在其服务器上执行的操作时，会出现另一个方便的情况。如果计划最终允许使用多种类型的服务器，则所有服务器都必须实现该接口。

很高兴知道
提取超类和提取接口之间有一定的相似之处。
提取接口只允许隔离通用接口，而不是通用代码。换句话说，如果类包含重复代码，则提取接口不会帮助您进行重复数据删除。
尽管如此，可以通过应用提取类将包含重复的行为移动到单独的组件并将所有工作委托给它来缓解这个问题。如果常见的行为规模很大，您总是可以使用提取超类。当然，这更容易，但请记住，如果您走这条路，您将只获得一个父类。

如何重构
创建一个空接口。
在接口中声明常用操作。
将必要的类声明为实现接口。
更改客户端代码中的类型声明以使用新接口。

 */
}

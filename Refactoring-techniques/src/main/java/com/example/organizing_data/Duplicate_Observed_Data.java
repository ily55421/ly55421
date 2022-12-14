package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:24
 * @Description TODO  重复观察数据 然后最好将数据分离到单独的类中，确保域类和 GUI 之间的连接和同步
 */
public class Duplicate_Observed_Data {
    /*
    问题
域数据是否存储在负责 GUI 的类中？

解决方案
然后最好将数据分离到单独的类中，确保域类和 GUI 之间的连接和同步。

为什么要重构
您希望为相同的数据拥有多个界面视图（例如，您同时拥有一个桌面应用程序和一个移动应用程序）。如果您无法将 GUI 与域分开，您将很难避免代码重复和大量错误。

好处
您在业务逻辑类和表示类之间划分职责（参见单一职责原则），这使您的程序更具可读性和可理解性。
如果需要添加新的界面视图，创建新的表示类；你不需要接触业务逻辑的代码（参见开放/封闭原则）。
现在不同的人可以处理业务逻辑和用户界面。

何时不使用
这种重构技术以其经典形式使用 Observer 模板执行，不适用于 Web 应用程序，其中所有类都在对 Web 服务器的查询之间重新创建。
尽管如此，将业务逻辑提取到单独的类中的一般原则也适用于 Web 应用程序。但这将根据您的系统设计方式使用不同的重构技术来实现。

如何重构
在 GUI 类中隐藏对域数据的直接访问。为此，最好使用自封装字段。因此，您为此数据创建了 getter 和 setter。
在 GUI 类事件的处理程序中，使用 setter 设置新的字段值。这将允许您将这些值传递给关联的域对象。
创建一个域类并将必要的字段从 GUI 类复制到它。为所有这些字段创建 getter 和 setter。

为这两个类创建一个观察者模式：
在域类中，创建一个数组来存储观察者对象（GUI对象），以及注册、删除和通知它们的方法。
在 GUI 类中，创建一个用于存储对域类的引用的字段以及 update() 方法，该方法将对对象的更改做出反应并更新 GUI 类中的字段值。请注意，值更新应直接在方法中建立，以避免递归。
在 GUI 类构造器中，创建域类的实例并将其保存在您创建的字段中。将 GUI 对象注册为域对象中的观察者。
在域类字段的设置器中，调用通知观察者的方法（即GUI类中的更新方法），以便将新值传递给GUI。
更改 GUI 类字段的设置器，以便它们直接在域对象中设置新值。注意确保值不是通过域类设置器设置的——否则将导致无限递归。

     */
}

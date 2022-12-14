package com.example.moving_features_betweeno_objects;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:10
 * @Description TODO  创建一个包含方法的新类，并使其成为实用程序类的子类或包装器。 工具类
 */
public class Introduce_Local_Extension {
    /*
    问题
实用程序类不包含您需要的某些方法。但是您不能将这些方法添加到类中。

解决方案
创建一个包含方法的新类，并使其成为实用程序类的子类或包装器。

为什么要重构
您正在使用的类没有您需要的方法。更糟糕的是，您无法添加这些方法（例如，因为这些类在第三方库中）。
有两种出路：
从相关类创建一个子类，包含方法并从父类继承其他所有内容。这种方式更容易，但有时会被实用程序类本身阻止（由于 final）。
创建一个包含所有新方法的包装类，其他地方将从实用程序类委托给相关对象。这种方法的工作量更大，因为您不仅需要代码来维护包装器和实用程序对象之间的关系，而且还需要大量简单的委托方法来模拟实用程序类的公共接口。

好处
通过将其他方法移动到单独的扩展类（包装器或子类），您可以避免使用不适合的代码混淆客户端类。程序组件更连贯，更可重用。

如何重构
创建一个新的扩展类：
选项 A：使其成为实用程序类的子项。
选项 B：如果您决定制作一个包装器，请在其中创建一个字段，用于存储将进行委托的实用程序类对象。使用此选项时，您还需要创建重复实用程序类的公共方法并包含对实用程序对象方法的简单委托的方法。
创建一个使用实用程序类的构造函数的参数的构造函数。
还要创建一个替代的“转换”构造函数，该构造函数仅在其参数中采用原始类的对象。这将有助于用扩展替换原始类的对象。
在类中创建新的扩展方法。将外来方法从其他类移动到此类，或者如果扩展中已经存在外来方法的功能，则删除外来方法。
在需要其功能的地方用新的扩展类替换实用程序类的使用。

     */
}

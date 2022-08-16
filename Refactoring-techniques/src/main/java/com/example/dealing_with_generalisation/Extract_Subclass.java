package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/16 8:37
 * @Description TODO 创建一个子类并在这些情况下使用它
 */
public class Extract_Subclass {
/*
问题
一个类具有仅在某些情况下使用的特性。

解决方案
创建一个子类并在这些情况下使用它。

为什么要重构
您的主类具有用于实现该类的某个罕见用例的方法和字段。虽然这种情况很少见，但类对此负责，将所有关联的字段和方法移动到一个完全独立的类是错误的。但是它们可以被移动到一个子类中，这正是我们将在这种重构技术的帮助下所做的。

好处
快速轻松地创建子类。
如果您的主类当前正在实现多个此类特殊情况，您可以创建多个单独的子类。

缺点
尽管看起来很简单，但如果您必须分离几个不同的类层次结构，继承可能会导致死胡同。例如，如果根据狗的大小和毛皮，您的 Dogs 类具有不同的行为，您可以梳理出两个层次结构：
按尺寸：大、中、小
按毛皮：光滑和蓬松
一切看起来都很好，除了当你需要创建一只又大又光滑的狗时就会出现问题，因为你只能从一个类中创建一个对象。也就是说，您可以通过使用 Compose 而不是 Inherit 来避免这个问题（请参阅策略模式）。换句话说，Dog 类将有两个组成字段，size 和 fur。您将从必要的类中插入组件对象到这些字段中。因此，您可以创建具有 LargeSize 和 ShaggyFur 的 Dog。

如何重构
从感兴趣的类创建一个新的子类。
如果您需要额外的数据来从子类创建对象，请创建一个构造函数并向其添加必要的参数。不要忘记调用构造函数的父实现。
查找对父类的构造函数的所有调用。当需要子类的功能时，将父构造函数替换为子类构造函数。
将必要的方法和字段从父类移动到子类。通过下推方法和下推场来做到这一点。首先移动方法更简单。这样，字段在整个过程中保持可访问性：在移动之前从父类访问，在移动完成后从子类本身访问。
子类准备好后，找到所有控制功能选择的旧字段。通过使用多态替换所有使用了这些字段的运算符来删除这些字段。一个简单的例子：在 Car 类中，你有字段 isElectricCar ，根据它，在 refuel() 方法中，汽车要么用汽油加油，要么用电充电。重构后，删除了 isElectricCar 字段，并且 Car 和 ElectricCar 类将拥有自己的 refuel() 方法实现。

 */
}

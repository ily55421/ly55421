package com.example.moving_features_betweeno_objects;

/**
 * @Author: linK
 * @Date: 2022/8/15 13:54
 * @Description TODO
 */
public class Extract_Class {
    /*
    问题
当一个班级完成两个班级的工作时，就会产生尴尬。

解决方案
相反，创建一个新类并将负责相关功能的字段和方法放入其中。

为什么要重构
课程一开始总是清晰易懂。他们做自己的工作，只管自己的事，不插手其他班级的工作。
但是随着程序的扩展，添加了一个方法，然后添加了一个字段……最终，一些类执行的职责比以往任何时候都多。

好处
这种重构方法将有助于保持对单一职责原则的遵守。您的类的代码将更加明显和易于理解。
单一职责类更可靠，更能容忍变化。例如，假设您有一个班级负责十种不同的事情。当您更改此课程以使其更好地用于一件事时，您可能会冒着为其他九个而破坏它的风险。

缺点
如果你用这种重构技术“过度使用”，你将不得不求助于内联类。

如何重构
在开始之前，请确定您希望如何准确地划分班级的职责。
创建一个新类以包含相关功能。
在旧类和新类之间建立关系。理想情况下，这种关系是单向的；这允许重用第二类而没有任何问题。尽管如此，如果您认为双向关系是必要的，则始终可以建立这种关系。
对您决定移动到新类的每个字段和方法使用移动字段和移动方法。对于方法，从私有的开始，以减少犯大量错误的风险。尝试一次重新定位一点点，并在每次移动后测试结果，以避免在最后堆积错误修复。
完成移动后，再看一下生成的类。可以重命名具有更改职责的旧类以提高清晰度。再次检查是否可以摆脱双向类关系（如果存在）。
还要考虑从外部对新类的可访问性。您可以通过将其设为私有，通过旧类中的字段对其进行管理，从而完全对客户端隐藏该类。或者，您可以通过允许客户端直接更改值来将其设为公开。您在此处的决定取决于当对新类中的值进行意外的直接更改时，旧类的行为有多安全。


     */
}

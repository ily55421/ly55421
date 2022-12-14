package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 15:02
 * @Description TODO 用子类替换类型代码
 */
public class Replace_Type_Code_with_Subclasses {
    /*
    问题
您有一个直接影响程序行为的编码类型（此字段的值触发条件中的各种代码）。

解决方案
为编码类型的每个值创建子类。然后将原始类中的相关行为提取到这些子类中。用多态替换控制流代码。

为什么要重构
这种重构技术是用类替换类型代码的一个更复杂的转折。
与第一种重构方法一样，您有一组简单的值，它们构成了一个字段的所有允许值。尽管这些值通常被指定为常量并且具有易于理解的名称，但它们的使用使您的代码非常容易出错，因为它们仍然是有效的原语。例如，您有一个在参数中接受这些值之一的方法。在某个时刻，该方法接收到的不是具有值“ADMIN”的常量 USER_TYPE_ADMIN，而是相同的小写字符串（“admin”），这将导致执行作者（您）不打算执行的其他操作。
在这里，我们正在处理控制流代码，例如条件 if、switch 和 ?:。换句话说，在这些运算符的条件中使用了具有编码值的字段（例如 $user->type === self::USER_TYPE_ADMIN）。如果我们在这里使用用类替换类型代码，所有这些控制流构造最好转移到负责数据类型的类。最终，这当然会创建一个与原始类型非常相似的类型类，但也存在相同的问题。

好处
删除控制流代码。将代码移动到适当的子类，而不是原始类中的笨重开关。这提高了对单一职责原则的遵守，并使程序总体上更具可读性。
如果您需要为编码类型添加新值，您需要做的就是添加一个新的子类而不触及现有代码（参见开放/封闭原则）。
通过用类替换类型代码，我们为编程语言级别的方法和字段的类型提示铺平了道路。使用编码类型中包含的简单数字或字符串值是不可能的。

何时不使用
如果您已经有一个类层次结构，则此技术不适用。在面向对象编程中，您不能通过继承创建双重层次结构。不过，您可以通过组合而不是继承来替换类型代码。为此，请使用将类型代码替换为状态/策略。
如果创建对象后类型代码的值会发生变化，请避免使用此技术。我们将不得不以某种方式即时替换对象本身的类，这是不可能的。不过，在这种情况下，另一种选择是用状态/策略替换类型代码。

如何重构
使用 Self Encapsulate Field 为包含类型代码的字段创建一个 getter。
将超类构造函数设为私有。创建一个与超类构造函数具有相同参数的静态工厂方法。它必须包含将采用编码类型的起始值的参数。根据这个参数，工厂方法将创建各种子类的对象。为此，您必须在其代码中创建一个大型条件，但至少，它是唯一真正需要的条件；否则，子类和多态就可以了。
为编码类型的每个值创建一个唯一的子类。其中，重新定义编码类型的getter，使其返回编码类型的对应值。
从超类中删除带有类型代码的字段。使其吸气剂抽象。
现在您有了子类，您可以开始将字段和方法从超类移动到相应的子类（借助下推字段和下推方法）。
当所有可能的东西都被移动后，使用用多态替换条件以一劳永逸地摆脱使用类型代码的条件。

     */
}

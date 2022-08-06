# 设计模式

设计模式（Design pattern）是重构解决方案

> 这点很重要，尤其是现在 B/S 一统天下的局面，过早考虑设计模式，得不偿失

设计模式（Design pattern）代表了最佳的实践，通常被面向对象的软件开发人员所采用

> 很多教程都说设计模式是被有经验的人使用，其实只要定义了一个类，或多或少都在使用它们，而不是有没有经验 只是有经验的人知道自己在使用设计模式，而且知道怎么做的更好

设计模式是软件开发人员在软件开发过程中面临复杂度问题的一般问题的解决方案

这些解决方案是众多软件开发人员经过相当长的一段时间的试验和错误总结出来的

> 设计模式是复杂度解决方案，不是小程序的解决方案(就一两个类文件，用设计模式那是增加复杂度)

## 记住

> 设计模式（Design pattern）是重构解决方案不是开发的解决方案
> 设计模式的 6 大原则才是开发的解决方案

设计模式是一套被反复使用的、多数人知晓的、经过分类编目的、代码设计经验的总结

使用设计模式是为了重用代码、让代码更容易被他人理解、保证代码可靠性

毫无疑问，设计模式于己于他人于系统都是多赢的，设计模式使代码编制真正工程化，设计模式是软件工程的基石，如同大厦的一块块砖石一样

项目中合理地运用设计模式可以完美地解决很多问题，每种模式在现实中都有相应的原理来与之对应，每种模式都描述了一个在我们周围不断重复发生的问题，以及该问题的核心解决方案，这也是设计模式能被广泛应用的原因

## 什么是 GOF（Gang of Four）？

1994 年，Erich Gamma、Richard Helm、Ralph Johnson 和 John Vlissides 四人合著出版了一本名为 **Design Patterns – Elements of Reusable Object-Oriented Software（中文译名：设计模式 – 可复用的面向对象软件元素）** 的书

> 书名真的是误导人啊… 为啥不添加上重构两字

该书首次提到了软件开发中设计模式的概念

四位作者合称 **GOF（四人帮，全拼 Gang of Four）**

他们所提出的设计模式主要是基于以下的面向对象设计原则

1、面向接口编程而不是对实现编程
		2、优先使用对象组合而不是继承

## 设计模式的使用

设计模式在软件开发中的两个主要用途

1、**开发人员的共同平台**

设计模式提供了一个标准的术语系统，且具体到特定的情景

例如，单例设计模式意味着使用单个对象，这样所有熟悉单例设计模式的开发人员都能使用单个对象，并且可以通过这种方式告诉对方，程序使用的是单例模式

2、**最佳的实践**

设计模式已经经历了很长一段时间的发展，它们提供了软件开发过程中面临的一般问题的最佳解决方案

学习这些模式有助于经验不足的开发人员通过一种简单快捷的方式来学习软件设计

## 关于范例

因为我用的是包机制来开发，所以引入了 `com.souyunku.tech.gof` 包，运行起来就有点复杂了 所以我们希望你使用 IDE 来测试范例，这样点击运行可以直接查看结果

如果是手动编译运行，比如工厂模式中的范例，则需要如下

```bash
$ javac -d . src/main/com/souyunku/tech/gof/FactoryPatternDemo.java
$ java  com.souyunku.tech.gof.FactoryPatternDemo
```

在范例中的 `编译运行以上 Java 范例` 指的就是这两条命令

本教程将通过 Java 范例，一步一步讲解学习设计模式的概念

## 谁适合阅读本教程？

无论您是新手，还是老手，本教程都值得一读

1、 对于那些具有丰富的开发经验的开发人员，学习设计模式有助于了解在软件开发过程中所面临的问题的最佳解决方案
2、 对于那些经验不足的开发人员，学习设计模式有助于通过一种简单快捷的方式来学习软件设计

> 总的来说，不推荐刚入门的开发者学习，哪怕把代码搞的一塌糊涂，也要先将功能完成，初学者，迈过坑是必然的，只有对自己编写的代码不满意，你才会体会到设计模式的重要性，也才能更加理解

## 阅读本教程前，我们希望需要了解的知识：

因为本教程的范例都是基于 Java 语言，所以我们希望你有一定的 Java 基础知识

## 附录：设计模式：系列文章

# 设计模式 – 四大类型

设计模式（Design pattern）是**重构解决方案**

根据书 Design Patterns – Elements of Reusable Object-Oriented Software（中文译名：设计模式 – 可复用的面向对象软件元素） 中和 J2EE 所提到的，总共有 23 +8 种设计模式

这些模式可以分为四大类：**创建型模式（Creational Patterns）、结构型模式（Structural Patterns）、行为型模式（Behavioral Patterns）、J2EE 设计模式**

## 1、创建型模式

这些设计模式提供了一种在创建对象的同时隐藏创建逻辑的方式，而不是使用 new 运算符直接实例化对象

这使得程序在判断针对某个给定实例需要创建哪些对象时更加灵活

包括

1、工厂模式（Factory Pattern）
		2、抽象工厂模式（Abstract Factory Pattern）
		3、单例模式（Singleton Pattern）
		4、建造者模式（Builder Pattern
		5、原型模式（Prototype Pattern）

## 2、结构型模式

这些设计模式关注类和对象的组合

继承的概念被用来组合接口和定义组合对象获得新功能的方式

包括

1、适配器模式（Adapter Pattern）
		2、桥接模式（Bridge Pattern）
		3、过滤器模式（Filter、Criteria Pattern）
		4、组合模式（Composite Pattern）
		5、装饰器模式（Decorator Pattern）
		6、外观模式（Facade Pattern）
		7、享元模式（Flyweight Pattern）
		8、代理模式（Proxy Pattern）

## 3、行为型模式

这些设计模式特别关注对象之间的通信

包括

1、责任链模式（Chain of Responsibility Pattern）
		2、命令模式（Command Pattern）
		3、解释器模式（Interpreter Pattern）
		4、迭代器模式（Iterator Pattern）
		5、中介者模式（Mediator Pattern）
		6、备忘录模式（Memento Pattern）
		7、观察者模式（Observer Pattern）
		8、状态模式（State Pattern）
		9、空对象模式（Null Object Pattern）

10. 策略模式（Strategy Pattern）
11. 模板模式（Template Pattern）
12. 访问者模式（Visitor Pattern）

## 4、J2EE 模式

这些设计模式特别关注表示层

这些模式是由 Sun Java Center 鉴定的

包括：

1、MVC 模式（MVC Pattern）
		2、业务代表模式（Business Delegate Pattern）
		3、组合实体模式（Composite Entity Pattern）
		4、数据访问对象模式（Data Access Object Pattern）
		5、前端控制器模式（Front Controller Pattern）
		6、拦截过滤器模式（Intercepting Filter Pattern）
		7、服务定位器模式（Service Locator Pattern）
		8、传输对象模式（Transfer Object Pattern）



# 设计模式 – 六大原则

在 23 +8 中设计模式中，我们提炼出了 六大面向对象设计原则

> 我们可以不知道那数量繁多的设计模式，但一定要记住这 六大设计原则

## 1. 开闭原则（Open Close Principle）

开闭原则的意思是： **对扩展开放，对修改关闭**

在程序需要进行拓展的时候，不能去修改原有的代码，实现一个热插拔的效果

简言之，是为了使程序的扩展性好，易于维护和升级

想要达到这样的效果，我们需要使用接口和抽象类，后面的具体设计中我们会提到这点

## 2. 里氏代换原则（Liskov Substitution Principle）

里氏代换原则是面向对象设计的基本原则之一

里氏代换原则中说，任何基类可以出现的地方，子类一定可以出现

LSP 是继承复用的基石，只有当派生类可以替换掉基类，且软件单位的功能不受到影响时，基类才能真正被复用，而派生类也能够在基类的基础上增加新的行为

里氏代换原则是对开闭原则的补充

实现开闭原则的关键步骤就是抽象化，而基类与子类的继承关系就是抽象化的具体实现，**所以里氏代换原则是对实现抽象化的具体步骤的规范**

## 3. 依赖倒转原则（Dependence Inversion Principle）

这个原则是开闭原则的基础，具体内容：**针对接口编程，依赖于抽象而不依赖于具体**

## 4. 接口隔离原则（Interface Segregation Principle）

这个原则的意思是：使用多个隔离的接口，比使用单个接口要好

它还有另外一个意思是：**降低类之间的耦合度**

由此可见，其实设计模式就是从大型软件架构出发、便于升级和维护的软件设计思想，它强调降低依赖，降低耦合

## 5. 迪米特法则，又称最少知道原则（Demeter Principle）

最少知道原则是指：**一个实体应当尽量少地与其他实体之间发生相互作用，使得系统功能模块相对独立。**

## 6. 合成复用原则（Composite Reuse Principle）

合成复用原则是指：**尽量使用合成/聚合的方式，而不是使用继承**





- [五、工厂模式 ( Factory Pattern )](https://tech.souyunku.com/?p=2661)
- [六、抽象工厂模式 ( Abstract Factory Pattern )](https://tech.souyunku.com/?p=2663)
- [七、单例模式 ( Singleton Pattern )](https://tech.souyunku.com/?p=2665)
- [八、建造者模式 ( Builder Pattern )](https://tech.souyunku.com/?p=2667)
- [九、原型模式 ( Prototype Pattern )](https://tech.souyunku.com/?p=2669)
- [十、适配器模式 ( Adapter Pattern )](https://tech.souyunku.com/?p=2671)
- [十一、桥接模式 ( Bridge Pattern )](https://tech.souyunku.com/?p=2673)
- [十二、过滤器模式 ( Filter Pattern )](https://tech.souyunku.com/?p=2675)
- [十三、组合模式 ( Composite Pattern )](https://tech.souyunku.com/?p=2677)
- [十四、装饰器模式 ( Decorator Pattern )](https://tech.souyunku.com/?p=2679)
- [十五、外观模式 ( Facade Pattern )](https://tech.souyunku.com/?p=2681)
- [十六、享元模式 ( Flyweight Pattern )](https://tech.souyunku.com/?p=2683)
- [十七、代理模式 ( Proxy Pattern )](https://tech.souyunku.com/?p=2685)
- [十八、责任链模式 ( Chain of Responsibility)](https://tech.souyunku.com/?p=2687)
- [十九、命令模式 ( Command Pattern )](https://tech.souyunku.com/?p=2689)
- [二十、解释器模式 ( Interpreter Pattern )](https://tech.souyunku.com/?p=2691)
- [二十一、迭代器模式 ( Iterator Pattern )](https://tech.souyunku.com/?p=2693)
- [二十二、中介者模式 ( Mediator Pattern )](https://tech.souyunku.com/?p=2695)
- [二十三、备忘录模式 ( Memento Pattern )](https://tech.souyunku.com/?p=2697)
- [二十四、观察者模式 ( Observer Pattern )](https://tech.souyunku.com/?p=2699)
- [二十五、状态模式 ( State Pattern )](https://tech.souyunku.com/?p=2701)
- [二十六、空对象模式 ( Null Object Pattern )](https://tech.souyunku.com/?p=2703)
- [二十七、策略模式 ( Strategy Pattern )](https://tech.souyunku.com/?p=2705)
- [二十八、模板模式 ( Template Pattern )](https://tech.souyunku.com/?p=2707)
- [二十九、访问者模式 ( Visitor Pattern )](https://tech.souyunku.com/?p=2709)
- [三十、MVC 模式](https://tech.souyunku.com/?p=2711)
- [三十一、业务代表模式(Business Delegate Pattern)](https://tech.souyunku.com/?p=2713)
- [三十二、组合实体模式 (Composite Entity Pattern)](https://tech.souyunku.com/?p=2715)
- [三十三、数据访问对象模式 ( Data Access Object )](https://tech.souyunku.com/?p=2717)
- [三十四、前端控制器模式(Front Controller Pattern)](https://tech.souyunku.com/?p=2719)
- [三十五、拦截过滤器模式 ( Intercepting Filter )](https://tech.souyunku.com/?p=2721)
- [三十六、服务定位器模式 (Service Locator Pattern)](https://tech.souyunku.com/?p=2723)
- [三十七、传输对象模式 ( Transfer Object Pattern )](https://tech.souyunku.com/?p=2725)
- [三十八、设计模式资源](https://tech.souyunku.com/?p=2727)
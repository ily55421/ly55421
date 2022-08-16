package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/15 18:03
 * @Description TODO
 */
public class Pull_Up_Constructor_Body {
    String name;
    String id;
    int grade;

    public Pull_Up_Constructor_Body() {

    }

    public Pull_Up_Constructor_Body(String name, String id) {
        this.name = name;
        this.id = id;
        this.grade = grade;
    }

    public Pull_Up_Constructor_Body(String name, String id, int grade) {
        this.name = name;
        this.id = id;
        this.grade = grade;
    }

    /*
问题
您的子类具有代码基本相同的构造函数。

解决方案
创建一个超类构造函数并将子类中相同的代码移动到它。在子类构造函数中调用超类构造函数。

为什么要重构
这种重构技术与 Pull Up 方法有何不同？
在Java中，子类不能继承构造函数，所以不能简单地对子类构造函数应用Pull Up Method，并在将所有构造函数代码移至超类后将其删除。除了在超类中创建构造函数外，还需要在子类中具有构造函数，并简单地委托给超类构造函数。
在 C++ 和 Java 中（如果您没有显式调用超类构造函数），超类构造函数会在子类构造函数之前自动调用，这使得只需要从子类构造函数的开头移动公共代码（因为您不会能够从子类构造函数中的任意位置调用超类构造函数）。
在大多数编程语言中，子类构造函数可以有自己的参数列表，不同于超类的参数。
TODO 因此，您应该只使用它真正需要的参数创建一个超类构造函数。

如何重构
在超类中创建构造函数。
提取每个子类的构造函数开头到超类构造函数的公共代码。在这样做之前，请尝试将尽可能多的通用代码移到构造函数的开头。
将对超类构造函数的调用放在子类构造函数的第一行。

 */
}

class Pull_Up_Constructor_Body_Before extends Pull_Up_Constructor_Body {
    public Pull_Up_Constructor_Body_Before(String name, String id, int grade) {
        this.name = name;
        this.id = id;
        this.grade = grade;
    }
    // ...


}

/**
 * 调用父类的构造体 方法
 */
class Pull_Up_Constructor_Body_After extends Pull_Up_Constructor_Body {
    public Pull_Up_Constructor_Body_After(String name, String id, int grade) {
        super(name, id);
        this.grade = grade;
    }
    // ...

}
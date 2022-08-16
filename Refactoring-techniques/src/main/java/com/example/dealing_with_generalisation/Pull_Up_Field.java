package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:57
 * @Description TODO  从子类中删除该字段并将其移至超类
 * 抽象有自己的一组重构技术，主要与沿类继承层次结构移动功能、创建新类和接口以及用委托代替继承相关，反之亦然。
 */
public class Pull_Up_Field {

/*
问题
两个类具有相同的字段。

解决方案
从子类中删除该字段并将其移至超类。

为什么要重构
子类分别成长和发展，导致出现相同（或几乎相同）的领域和方法。

好处
消除子类中字段的重复。
简化后续从子类到超类的重复方法（如果存在）的重定位。

如何重构
确保这些字段用于子类中的相同需求。
如果字段具有不同的名称，请给它们相同的名称并替换现有代码中对字段的所有引用。
在超类中创建一个同名的字段。请注意，如果字段是私有的，则超类字段应该受到保护。
从子类中删除字段。
您可能需要考虑为新字段使用自封装字段，以便将其隐藏在访问方法后面。


 */
}

class Unit_Before {
    class Solider extends Unit_Before{
        private int age;
    }

    class Tanks extends Unit_Before{
        private int age;
    }
}

class Unit_After {
    private int age;

    class Solider extends Unit_After{
    }

    class Tanks extends Unit_After{
    }
}
package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:30
 * @Description TODO 用符号常量替换魔法数字
 */
public class Replace_Magic_Number_with_Symbolic_Constant {
    /*
    问题
您的代码使用了一个具有特定含义的数字。

解决方案
将此数字替换为具有人类可读名称的常量，该名称解释了数字的含义。

为什么要重构
幻数是在源中遇到但没有明显含义的数值。这种“反模式”使得理解程序和重构代码变得更加困难。
然而，当你需要改变这个神奇的数字时，就会出现更多的困难。查找和替换对此不起作用：相同的数字可能在不同的地方用于不同的目的，这意味着您必须验证使用该数字的每一行代码。

好处
符号常量可以作为其值含义的实时文档。
更改常量的值比在整个代码库中搜索该数字要容易得多，而且不会有意外更改其他地方用于不同目的的相同数字的风险。
减少代码中数字或字符串的重复使用。当值复杂且长时（例如 3.14159 或 0xCAFEBABE），这一点尤其重要。

很高兴知道
并非所有数字都是神奇的。
如果数字的用途很明显，则无需替换它。一个经典的例子是：
for (i = 0; i < сcount; i++) ... ;

备择方案
有时可以用方法调用替换幻数。例如，如果您有一个表示集合中元素数量的幻数，则不需要使用它来检查集合的最后一个元素。相反，使用标准方法来获取集合长度。
幻数有时用作类型代码。假设您有两种类型的用户，并且您使用类中的数字字段来指定哪一种：管理员为 1，普通用户为 2。
在这种情况下，您应该使用其中一种重构方法来避免类型代码：
用类替换类型代码
用子类替换类型代码
用状态/策略替换类型代码

如何重构
声明一个常量并将幻数的值赋给它。
找到所有提到的幻数。
对于您找到的每个数字，请仔细检查这种特殊情况下的幻数是否与常量的用途相对应。如果是，请将数字替换为您的常数。这是一个重要的步骤，因为相同的数字可能意味着完全不同的东西（并根据情况用不同的常数代替）。


     */
}


class Replace_Magic_Number_with_Symbolic_Constant_Before{
    double potentialEnergy(double mass, double height) {
        return mass * height * 9.81;
    }
}

/**
 * 用常量替换魔法数字
 */
class Replace_Magic_Number_with_Symbolic_Constant_After{
    /**
     * 因数常量
     */
    static final double GRAVITATIONAL_CONSTANT = 9.81;

    double potentialEnergy(double mass, double height) {
        return mass * height * GRAVITATIONAL_CONSTANT;
    }
}
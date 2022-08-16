package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:03
 * @Description TODO 参数化方法
 */
public class Parameterize_Method {
    /*
    问题
多个方法执行相似的操作，只是它们的内部值、数字或操作不同。

解决方案
通过使用将传递必要的特殊值的参数组合这些方法。

为什么要重构
如果你有类似的方法，你可能有重复的代码，这会带来所有的后果。
此外，如果您需要添加此功能的另一个版本，则必须创建另一个方法。相反，您可以简单地使用不同的参数运行现有方法。

缺点
有时这种重构技术可能会走得太远，导致一个冗长而复杂的通用方法而不是多个更简单的方法。
将功能的激活/停用移动到参数时也要小心。这最终会导致创建一个大型条件运算符，需要通过使用显式方法替换参数来处理该运算符。

如何重构
通过应用提取方法，创建一个带有参数的新方法并将其移动到对所有类都相同的代码中。请注意，有时只有某些方法实际上是相同的。在这种情况下，重构包括仅将相同部分提取到新方法中。
在新方法的代码中，将特殊/不同的值替换为参数。
对于每个旧方法，找到调用它的位置，将这些调用替换为对包含参数的新方法的调用。然后删除旧方法。


     */
}

abstract class Parameterize_Method_Before {
    abstract void fivePercentRaise();

    abstract void tenPercentRaise();
}

/**
 * 将多个相同类型方法 参数抽取成通用方法的参数
 */
abstract class Parameterize_Method_After {
    abstract void Raise(int percentage);

}
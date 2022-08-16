package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:12
 * @Description TODO
 */
public class Self_Encapsulate_Field {
    /*
    自封装不同于普通的封装字段：这里给出的重构技术是在私有字段上执行的。

    问题
您可以直接访问类中的私有字段。

解决方案
为该字段创建一个 getter 和 setter，并仅使用它们来访问该字段。

为什么要重构
有时直接访问类中的私有字段不够灵活。
您希望能够在进行第一次查询时启动字段值，或者在分配字段的新值时对它们执行某些操作，或者可能在子类中以各种方式执行所有这些操作。

好处
对字段的间接访问是指通过访问方法（getter 和 setter）对字段进行操作。这种方法比直接访问字段要灵活得多。
首先，您可以在设置或接收字段中的数据时执行复杂的操作。字段值的延迟初始化和验证很容易在字段 getter 和 setter 中实现。
其次，更重要的是，您可以在子类中重新定义 getter 和 setter。
您可以选择根本不为字段实现 setter。该字段值将仅在构造函数中指定，从而使该字段在整个对象生命周期内都不可更改。

缺点
当使用直接访问字段时，代码看起来更简单、更美观，尽管灵活性降低了。

如何重构
为该字段创建一个 getter（和可选的 setter）。它们应该受到保护（protected）或公开（public）。
查找该字段的所有直接调用，并将它们替换为 getter 和 setter 调用。

     */
}
class Self_Encapsulate_Field_Before{
    private int low, high;
    boolean includes(int arg) {
        return arg >= low && arg <= high;
    }
}

/**
 * 私有字段封装
 */
class Self_Encapsulate_Field_After{
    private int low, high;
    boolean includes(int arg) {
        return arg >= getLow() && arg <= getHigh();
    }
    int getLow() {
        return low;
    }
    int getHigh() {
        return high;
    }
}
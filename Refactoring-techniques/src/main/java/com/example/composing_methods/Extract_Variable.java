package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:37
 * @Description TODO 将复杂表达式 抽取成变量
 */
public class Extract_Variable {
    /*

问题
你有一个难以理解的表达方式。

解决方案
将表达式的结果或其部分放在不言自明的单独变量中。

为什么要重构
提取变量的主要原因是通过将复杂表达式划分为中间部分来使复杂表达式更易于理解。
这些可能是：
if() 运算符的条件或 ?: 运算符的一部分在基于 C 的语言中
没有中间结果的长算术表达式
长的多部分线
如果您发现提取的表达式在代码的其他位置使用，则提取变量可能是执行提取方法的第一步。

好处
更易读的代码！尝试给提取的变量起好名字，以响亮而清晰地宣布变量的用途。更具可读性，更少冗长的评论。
选择 customerTaxValue、cityUnemploymentRate、clientSalutationString 等名称。

缺点
您的代码中存在更多变量。但这与阅读代码的便利性相抵消。

如何重构
在相关表达式之前插入一个新行并在那里声明一个新变量。将复杂表达式的一部分分配给该变量。
用新变量替换表达式的那部分。
对表达式的所有复杂部分重复该过程。
     */
}
class Extract_Variable_Before{
    private String platform;
    private String browser;
    private int resize;
    void renderBanner() {
        if ((platform.toUpperCase().indexOf("MAC") > -1) &&
                (browser.toUpperCase().indexOf("IE") > -1) &&
                wasInitialized() && resize > 0 )
        {
            // do something
        }
    }

    private boolean wasInitialized() {
        return false;
    }
}

class Extract_Variable_After{
    private String platform;
    private String browser;
    private int resize;

    /**
     * 将判断表达式 抽取成变量
     */
    void renderBanner() {
        final boolean isMacOs = platform.toUpperCase().indexOf("MAC") > -1;
        final boolean isIE = browser.toUpperCase().indexOf("IE") > -1;
        final boolean wasResized = resize > 0;

        if (isMacOs && isIE && wasInitialized() && wasResized) {
            // do something
        }

    }
    private boolean wasInitialized() {
        return false;
    }
}


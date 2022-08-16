package com.example.simplifying_conditional_expressions;

/**
 * @Author: linK
 * @Date: 2022/8/15 15:28
 * @Description TODO 合并条件表达式
 */
public class Consolidate_Conditional_Expression {
    /*
    问题
您有多个导致相同结果或操作的条件。

解决方案
将所有这些条件合并到一个表达式中。

为什么要重构
您的代码包含许多执行相同操作的交替运算符。目前尚不清楚为什么运营商会被拆分。
合并的主要目的是将条件提取到单独的方法中以更清晰。

好处
消除重复的控制流代码。组合具有相同“目的地”的多个条件有助于表明您只进行了一项复杂的检查，导致一项操作。
通过合并所有运算符，您现在可以将这个复杂的表达式隔离在一个新方法中，该方法的名称可以解释条件的目的。

如何重构
在重构之前，请确保条件没有任何“副作用”或以其他方式修改某些内容，而不是简单地返回值。副作用可能隐藏在运算符本身内部执行的代码中，例如当基于条件的结果将某些内容添加到变量时。
使用 and 和 or 将条件合并到一个表达式中。作为合并时的一般规则：
嵌套条件使用 and 连接。
连续的条件句用 or 连接。
对运算符条件执行提取方法，并为该方法指定一个反映表达式用途的名称。

     */
}

class Consolidate_Conditional_Expression_Before {
    private int seniority;
    private int monthsDisabled;
    private boolean isPartTime;

    double disabilityAmount() {
        if (seniority < 2) {
            return 0;
        }
        if (monthsDisabled > 12) {
            return 0;
        }
        if (isPartTime) {
            return 0;
        }
        // Compute the disability amount.
        // ...
        return 0;
    }
}

/**
 * 合并表达式内容
 */
class Consolidate_Conditional_Expression_After {
    private int seniority;
    private int monthsDisabled;
    private boolean isPartTime;

    double disabilityAmount() {
        if (isNotEligibleForDisability()) {
            return 0;
        }
        // Compute the disability amount.
        // ...
        return 0;
    }

    /**
     * 不符合资格条件合并
     *
     * @return
     */
    private boolean isNotEligibleForDisability() {
        return seniority < 2 || monthsDisabled > 12 || isPartTime;
    }
}

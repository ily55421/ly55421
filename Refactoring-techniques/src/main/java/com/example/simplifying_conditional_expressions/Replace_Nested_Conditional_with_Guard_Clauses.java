package com.example.simplifying_conditional_expressions;

/**
 * @Author: linK
 * @Date: 2022/8/15 16:03
 * @Description TODO 用保护子句替换嵌套条件
 */
public class Replace_Nested_Conditional_with_Guard_Clauses {
    /*
    问题
您有一组嵌套条件，很难确定代码执行的正常流程。

解决方案
将所有特殊检查和边缘情况隔离到单独的子句中，并将它们放在主要检查之前。理想情况下，您应该有一个一个接一个的“平面”条件列表。

为什么要重构
发现“来自地狱的条件”相当容易。每一层嵌套的缩进形成一个箭头，指向右边的痛苦和不幸的方向：
if () {
    if () {
        do {
            if () {
                if () {
                    if () {
                        ...
                    }
                }
                ...
            }
            ...
        }
        while ();
        ...
    }
    else {
        ...
    }
}

很难弄清楚每个条件的作用和方式，因为“正常”的代码执行流程并不是很明显。这些条件表明了仓促的演变，每个条件都作为权宜之计而添加，没有考虑优化整体结构。
为了简化情况，将特殊情况隔离到单独的条件中，如果保护子句为真，则立即结束执行并返回空值。实际上，您在这里的任务是使结构平坦。

如何重构
尝试消除代码的副作用——从修饰符中分离查询可能有助于达到此目的。该解决方案对于下面描述的改组是必要的。
隔离所有导致调用异常或从方法中立即返回值的保护子句。将这些条件放在方法的开头。
重新排列完成并成功完成所有测试后，查看是否可以将合并条件表达式用于导致相同异常或返回值的保护子句。

     */
}
class Replace_Nested_Conditional_with_Guard_Clauses_Before{
    boolean isDead;
    boolean isSeparated;
    boolean isRetired;

    public double getPayAmount() {
        double result;
        if (isDead){
            result = deadAmount();
        }
        else {
            if (isSeparated){
                result = separatedAmount();
            }
            else {
                if (isRetired){
                    result = retiredAmount();
                }
                else{
                    result = normalPayAmount();
                }
            }
        }
        return result;
    }




    private double separatedAmount() {
        return 0;

    }

    private double normalPayAmount() {
        return 0;
    }

    private double retiredAmount() {
        return 0;

    }

    private double deadAmount() {
        return 0;

    }
}

class Replace_Nested_Conditional_with_Guard_Clauses_After{
    boolean isDead;
    boolean isSeparated;
    boolean isRetired;

    public double getPayAmount() {
        if (isDead){
            return deadAmount();
        }
        if (isSeparated){
            return separatedAmount();
        }
        if (isRetired){
            return retiredAmount();
        }
        return normalPayAmount();
    }



    private double separatedAmount() {
        return 0;

    }

    private double normalPayAmount() {
        return 0;
    }

    private double retiredAmount() {
        return 0;

    }

    private double deadAmount() {
        return 0;

    }
}
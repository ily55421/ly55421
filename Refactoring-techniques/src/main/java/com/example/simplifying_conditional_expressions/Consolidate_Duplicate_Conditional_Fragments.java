package com.example.simplifying_conditional_expressions;

/**
 * @Author: linK
 * @Date: 2022/8/15 15:34
 * @Description TODO 合并重复的条件片段
 */
public class Consolidate_Duplicate_Conditional_Fragments {
    /*
    问题
在条件的所有分支中都可以找到相同的代码。

解决方案
将代码移到条件之外。

为什么要重构
重复代码出现在条件的所有分支中，通常是条件分支中代码演变的结果。团队发展可能是其中的一个促成因素。

好处
代码重复数据删除。

如何重构
如果重复的代码位于条件分支的开头，请将代码移动到条件之前的位置。
如果代码在分支的末尾执行，则将其放在条件之后。
如果重复代码随机位于分支内部，首先尝试将代码移动到分支的开头或结尾，这取决于它是否会改变后续代码的结果。
如果合适且重复代码超过一行，请尝试使用提取方法。
     */
}

class Consolidate_Duplicate_Conditional_Fragments_Before {
    private double total;

    void toSend(double price) {
        if (isSpecialDeal()) {
            total = price * 0.95;
            send();
        } else {
            total = price * 0.98;
            send();
        }
    }

    private boolean isSpecialDeal() {
        return true;
    }

    private void send() {
        System.out.println("发送");
    }

}

/**
 * 合并相同的条件表达式
 */
class Consolidate_Duplicate_Conditional_Fragments_After {
    private double total;

    void toSend(double price) {
        if (isSpecialDeal()) {
            total = price * 0.95;
        } else {
            total = price * 0.98;
        }
        send();

    }

    private boolean isSpecialDeal() {
        return true;
    }

    private void send() {
        System.out.println("发送");
    }
}

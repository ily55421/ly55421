package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:53
 * @Description TODO
 */
public class Replace_Exception_with_Test {

/*
问题
你在一个简单的测试就可以完成工作的地方抛出一个异常？

解决方案
用条件测试替换异常。

为什么要重构
应使用异常来处理与意外错误相关的不规则行为。它们不应该作为测试的替代品。如果可以通过在运行前简单地验证条件来避免异常，那么就这样做。应为真正的错误保留异常。
比如你进入了雷区，在那里触发了地雷，导致异常；异常被成功处理，你被从空中升到了雷区之外的安全地带。但是你可以通过简单地阅读雷区前面的警告标志来避免这一切。

好处
一个简单的条件有时可能比异常处理代码更明显。

如何重构
为边缘情况创建条件并将其移动到 try/catch 块之前。
从这个条件内的 catch 部分移动代码。
在 catch 部分，放置用于引发通常未命名异常的代码并运行所有测试。
TODO 如果在测试期间没有抛出异常，请去掉 try/catch 操作符。

 */
}

class Replace_Exception_with_Test_Before {
    double[] values = new double[]{1.1};

    double getValueForPeriod(int periodNumber) {
        try {
            return values[periodNumber];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }
}

class Replace_Exception_with_Test_After {
    double[] values = new double[]{1.1};

    /**
     * 增加边界判断而不是直接抛出异常
     *
     * @param periodNumber
     * @return
     */
    double getValueForPeriod(int periodNumber) {
        if (periodNumber >= values.length) {
            return 0;
        }
        return values[periodNumber];
    }
}

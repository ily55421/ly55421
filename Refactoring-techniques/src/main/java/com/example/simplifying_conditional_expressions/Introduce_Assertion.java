package com.example.simplifying_conditional_expressions;

import org.junit.Assert;

/**
 * @Author: linK
 * @Date: 2022/8/15 16:38
 * @Description TODO  引入断言
 */
public class Introduce_Assertion {
    /*

问题
要使部分代码正常工作，某些条件或值必须为真。

解决方案
用特定的断言检查替换这些假设。

为什么要重构
假设代码的一部分假设了一些事情，例如，对象的当前条件或参数或局部变量的值。通常，除非发生错误，否则此假设将始终成立。
通过添加相应的断言使这些假设变得明显。与方法参数中的类型提示一样，这些断言可以充当代码的实时文档。
作为查看代码在何处需要断言的指南，请检查描述特定方法工作条件的注释。

好处
如果假设不正确，因此代码给出了错误的结果，最好在这导致致命后果和数据损坏之前停止执行。这也意味着您在设计执行程序测试的方法时忽略了编写必要的测试。

缺点
有时异常比简单的断言更合适。您可以选择异常的必要类，并让剩余的代码正确处理它。
什么时候异常比简单的断言更好？如果异常可能是由用户或系统的操作引起的，您可以处理该异常。另一方面，普通的未命名和未处理的异常基本上等同于简单的断言——你不处理它们，它们完全是由一个不应该发生的程序错误引起的。

如何重构
当您看到假设条件时，请为此条件添加断言以确保。
添加断言不应改变程序的行为。
不要过度使用对代码中所有内容的断言。仅检查代码正确运行所必需的条件。如果您的代码即使在特定断言为假的情况下也能正常工作，您可以安全地删除该断言。

     */
}


class Introduce_Assertion_Before {
    private Introduce_Assertion_Before primaryProject = new Introduce_Assertion_Before();
    private double expenseLimit = 0;
    private final static double NULL_EXPENSE = 0;

    double getExpenseLimit() {
        // Should have either expense limit or
        // a primary project.
        return (expenseLimit != NULL_EXPENSE) ?
                expenseLimit :
                primaryProject.getMemberExpenseLimit();
    }

    double getMemberExpenseLimit() {
        return 0;
    }
}

class Introduce_Assertion_After {
    private Introduce_Assertion_Before primaryProject = new Introduce_Assertion_Before();
    private double expenseLimit = 0;
    private final static double NULL_EXPENSE = 0;

    double getExpenseLimit() {
        /**
         * 引入断言先进行 条件参数校验
         */
        Assert.assertTrue(expenseLimit != NULL_EXPENSE || primaryProject != null);

        return (expenseLimit != NULL_EXPENSE) ?
                expenseLimit :
                primaryProject.getMemberExpenseLimit();
    }
}
package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:23
 * @Description TODO  更易读的代码！确保给新方法起一个描述方法用途的名称：createOrder()、renderCustomerInfo() 等。
 *
 * 减少代码重复。通常，在方法中找到的代码可以在程序的其他地方重用。因此，您可以使用对新方法的调用来替换重复项。
 *
 * 隔离代码的独立部分，这意味着错误的可能性较小（例如，如果修改了错误的变量）。
 */
public class ExtractMethod {
/*
问题
您有一个可以组合在一起的代码片段。
解决方案
将此代码移动到一个单独的新方法（或函数）中，并将旧代码替换为对该方法的调用。

为什么要重构

在方法中找到的行越多，就越难弄清楚该方法的作用。这是这次重构的主要原因。
除了消除代码中的粗糙边缘之外，提取方法也是许多其他重构方法中的一个步骤。

好处
更易读的代码！请务必为新方法指定一个描述方法用途的名称：createOrder()、renderCustomerInfo() 等。
减少代码重复。通常，在方法中找到的代码可以在程序的其他地方重用。因此，您可以使用对新方法的调用来替换重复项。
隔离代码的独立部分，这意味着错误的可能性较小（例如，如果修改了错误的变量）。

如何重构
创建一个新方法并以使其目的不言而喻的方式命名。
将相关代码片段复制到您的新方法中。从旧位置删除片段，并在那里调用新方法。
查找此代码片段中使用的所有变量。如果它们在片段内部声明而不在片段外部使用，只需保持不变——它们将成为新方法的局部变量。
如果变量是在您提取的代码之前声明的，则需要将这些变量传递给新方法的参数，以便使用之前包含在其中的值。有时通过用查询替换临时来更容易摆脱这些变量。
如果您发现提取的代码中的局部变量以某种方式发生了变化，这可能意味着稍后在您的 main 方法中将需要此更改的值。再检查一遍！如果确实如此，则将此变量的值返回给 main 方法以保持一切正常。


 */
}
class Extract_Method_Before{
    private final static String name = "";

    // before
    public void printOwing(){
        printBanner();

        // Print details.
        System.out.println("name: " + name);
        System.out.println("amount: " + getOutstanding());
    }

    private String getOutstanding() {
        return "";
    }

    private void printBanner() {
    }
}
/**
 * 抽取方法 改进
 */
class Extract_Method_After{
    private final static String name = "";

    /**
     * 抽取方法细节 用于对应实现
     */
    void printOwing() {
        printBanner();
        printDetails(getOutstanding());
    }

    private double getOutstanding() {
        return 0.0;
    }

    private void printBanner() {
    }

    void printDetails(double outstanding) {
        System.out.println("name: " + name);
        System.out.println("amount: " + outstanding);
    }



}
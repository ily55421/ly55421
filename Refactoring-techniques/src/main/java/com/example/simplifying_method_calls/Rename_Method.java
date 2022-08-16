package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 16:46
 * @Description TODO 重命名方法
 */
public class Rename_Method {
    /*
    问题
方法的名称并不能解释该方法的作用。

解决方案
重命名方法。

为什么要重构
可能一个方法从一开始就没有很好地命名——例如，有人仓促地创建了该方法，并且没有给予适当的注意来很好地命名它。
或者，也许该方法一开始命名良好，但随着其功能的增长，方法名称不再是一个好的描述符。

好处
代码可读性。尝试给新方法起一个反映其作用的名称。像 createOrder()、renderCustomerInfo() 等。

如何重构
查看该方法是在超类还是子类中定义。如果是这样，您也必须重复这些课程中的所有步骤。
下一个方法对于在重构过程中维护程序的功能很重要。使用新名称创建一个新方法。将旧方法的代码复制到其中。删除旧方法中的所有代码，代替它，插入对新方法的调用。
找到对旧方法的所有引用，并将它们替换为对新方法的引用。
删除旧方法。
TODO 如果旧方法是公共接口的一部分，请不要执行此步骤。相反，将旧方法标记为已弃用。

     */
}

class Rename_Method_Before {
    @Deprecated
    String getsnm() {
        return "";
    }
}

/**
 * 方法命名要符合本身的含义
 */
class Rename_Method_After {
    String getSecondName() {
        return "";
    }
}

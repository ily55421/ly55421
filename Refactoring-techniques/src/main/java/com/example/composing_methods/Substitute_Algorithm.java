package com.example.composing_methods;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/15 13:47
 * @Description TODO  用新算法替换实现算法的方法体。
 */
public class Substitute_Algorithm {
    /*
    问题
所以你想用新的算法替换现有的算法？

解决方案
用新算法替换实现算法的方法体。

为什么要重构
逐步重构并不是改进程序的唯一方法。有时，一个方法充满了问题，因此更容易拆除该方法并重新开始。
也许你已经找到了一种更简单、更高效的算法。如果是这种情况，您应该简单地用新算法替换旧算法。
随着时间的推移，您的算法可能会被合并到一个知名的库或框架中，您希望摆脱自己的独立实现，以简化维护。
您的程序的要求可能会发生很大的变化，以至于您现有的算法无法用于该任务。

如何重构
确保您已尽可能简化现有算法。使用提取方法将不重要的代码移动到其他方法。算法中的移动部件越少，更换就越容易。
以新方法创建新算法。用新算法替换旧算法并开始测试程序。
如果结果不匹配，则返回旧实现并比较结果。确定差异的原因。虽然原因通常是旧算法中的错误，但更有可能是由于某些东西在新算法中不起作用。
当所有测试都成功完成后，删除旧算法就好了！

     */
}

/**
 * 算法改进之前
 */
class Substitute_Algorithm_Before{
    String foundPerson(String[] people){
        for (int i = 0; i < people.length; i++) {
            if (people[i].equals("Don")){
                return "Don";
            }
            if (people[i].equals("John")){
                return "John";
            }
            if (people[i].equals("Kent")){
                return "Kent";
            }
        }
        return "";
    }

}

/**
 * 算法改进之后
 */
class Substitute_Algorithm_After{
    String foundPerson(String[] people){
        List candidates =
                Arrays.asList(new String[] {"Don", "John", "Kent"});
        for (int i=0; i < people.length; i++) {
            if (candidates.contains(people[i])) {
                return people[i];
            }
        }
        return "";
    }
}
package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/15 18:09
 * @Description TODO 下放方法   使用数  多则向上抽取，少则下放到具体的调用子类
 */
public class Push_Down_Method {
/*
问题
行为是否在仅由一个（或几个）子类使用的超类中实现？

解决方案
将此行为移至子类。

为什么要重构
起初，某种方法意味着对所有类都是通用的，但实际上只在一个子类中使用。当计划的功能未能实现时，可能会发生这种情况。
这种情况也可能发生在从类层次结构中部分提取（或删除）功能之后，留下仅在一个子类中使用的方法。
如果您发现多个子类（但不是所有子类）需要一个方法，则创建一个中间子类并将该方法移至该子类可能会很有用。这可以避免由于将方法下推到所有子类而导致的代码重复。

好处
提高类的连贯性。方法位于您希望看到它的位置。

如何重构
在子类中声明该方法并从超类中复制其代码。
从超类中删除该方法。
找到所有使用该方法的地方，并验证它是从必要的子类中调用的。

 */
}

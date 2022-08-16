package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:12
 * @Description TODO 保留整个对象
 */
public class Preserve_Whole_Object {
/*
问题
您从一个对象中获取多个值，然后将它们作为参数传递给一个方法。

解决方案
相反，请尝试传递整个对象。

为什么要重构
问题是每次调用你的方法之前，都必须调用future参数对象的方法。如果这些方法或为该方法获取的数据量发生变化，您将需要在程序中仔细找到十几个这样的地方，并在每个地方实现这些更改。
应用这种重构技术后，用于获取所有必要数据的代码将存储在一个地方——方法本身。

好处
您看到的不是大杂烩的参数，而是具有易于理解的名称的单个对象。
如果该方法需要来自对象的更多数据，则无需重写所有使用该方法的地方——只需在方法本身内部。

缺点
有时这种转换会导致方法变得不那么灵活：以前该方法可以从许多不同的来源获取数据，但现在，由于重构，我们将其使用限制为仅具有特定接口的对象。

如何重构
在对象的方法中创建一个参数，您可以从中获取必要的值。
现在开始从方法中一一移除旧参数，替换为对参数对象相关方法的调用。每次替换参数后测试程序。
从方法调用之前的参数对象中删除 getter 代码。

 */
}

class Preserve_Whole_Object_Before {
    private DaysTempRange daysTempRange;
    private Plan plan;
    int low = daysTempRange.getLow();
    int high = daysTempRange.getHigh();
    boolean withinPlan = plan.withinRange(low, high);


    class Plan {
        boolean withinRange(int low, int high) {
            return true;
        }

        /**
         * 直接传递整个对象 而不进行拆分
         *
         * @param daysTempRange
         * @return
         */
        boolean withinRange(DaysTempRange daysTempRange) {
            return true;
        }
    }
}

/**
 * 保留整个对象 传参进行计算
 */
class Preserve_Whole_Object_After {
    private DaysTempRange daysTempRange;
    private Preserve_Whole_Object_Before.Plan plan;


    boolean withinPlan = plan.withinRange(daysTempRange);


}

class DaysTempRange {
    int low;
    int high;

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }
}
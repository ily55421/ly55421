package com.example.simplifying_conditional_expressions;

/**
 * @Author: linK
 * @Date: 2022/8/15 16:28
 * @Description TODO 引入空对象
 */
public class Introduce_Null_Object {
    /*
    问题
由于某些方法返回 null 而不是真实对象，因此您在代码中需要对 null 进行许多检查。

解决方案
而不是 null，而是返回一个显示默认行为的 null 对象。

为什么要重构
对 null 的数十次检查使您的代码更长更丑陋。

缺点
摆脱条件的代价是创建另一个新类。

如何重构
从有问题的类中，创建一个将执行空对象角色的子类。
在这两个类中，创建方法 isNull()，它将为空对象返回 true，为真实类返回 false。
查找代码可能返回 null 而不是真实对象的所有位置。更改代码，使其返回一个空对象。
查找所有将真实类的变量与 null 进行比较的地方。将这些检查替换为对 isNull() 的调用。
如果在值不等于 null 时在这些条件中运行原始类的方法，请在 null 类中重新定义这些方法，并将条件的 else 部分的代码插入那里。
然后您可以删除整个条件和不同的行为将通过多态性实现。
如果事情不是那么简单并且无法重新定义方法，请查看是否可以简单地将在 null 值的情况下应该执行的运算符提取到 null 对象的新方法中。
默认调用这些方法而不是else中的旧代码作为操作。

     */
}
class  Introduce_Null_Object_Before{
    private String plan;
    void plan(BillingPlan customer){
        if (customer == null) {
            plan = BillingPlan.basic();
        }
        else {
            plan = customer.getPlan();
        }
    }

}
class Introduce_Null_Object_After{
    private String plan;
    void plan(BillingPlan customer){
        Order order = new Order();
        // Replace null values with Null-object.
        customer = (order.customer != null) ?
                order.customer : new NullCustomer();

// Use Null-object as if it's normal subclass.
        plan = customer.getPlan();
    }

}
class NullCustomer extends BillingPlan {
    boolean isNull() {
        return true;
    }

    @Override
    public String getPlan() {
        return new NullCustomer().toString();
    }
    // Some other NULL functionality.
}

class Order{
    public BillingPlan customer;
}

class  BillingPlan{
    public static String basic() {
        return "";
    }
    public String getPlan(){
        return "";
    }
}
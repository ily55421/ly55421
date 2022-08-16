package com.example.composing_methods;

/**
 * @Author: linK
 * @Date: 2022/8/15 10:43
 * @Description TODO
 */
public class Inline_Temp {
    /*

    问题
您有一个临时变量，它被分配了一个简单表达式的结果，仅此而已。

解决方案
用表达式本身替换对变量的引用。

为什么要重构
内联局部变量几乎总是用作 Replace Temp with Query 的一部分或为 Extract Method 铺平道路。

好处
这种重构技术本身几乎没有任何好处。但是，如果将变量分配给方法的结果，则可以通过删除不必要的变量来略微提高程序的可读性。

缺点
有时，看似无用的临时文件被用于缓存多次重复使用的昂贵操作的结果。因此，在使用这种重构技术之前，请确保简单性不会以性能为代价。

如何重构
查找所有使用该变量的地方。而不是变量，使用已分配给它的表达式。
删除变量的声明及其赋值行。

     */
}


class Inline_Temp_Before{
    boolean hasDiscount(Order order) {
        double basePrice = order.basePrice();
        return basePrice > 1000;
    }
}

/**
 *  取消内部多余 临时变量 直接返回简单表达式的结果
 */
class Inline_Temp_After{
    boolean hasDiscount(Order order) {
        return order.basePrice() > 1000;
    }
}

class Order {
    public int  basePrice(){
        return 500;
    }
}
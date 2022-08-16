package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:38
 * @Description TODO  移除set方法
 */
public class Remove_Setting_Method {
/*
问题
字段的值应仅在创建时设置，之后不得更改。

解决方案
所以删除设置字段值的方法。

为什么要重构
您希望防止对字段值进行任何更改。

如何重构
字段的值只能在构造函数中更改。如果构造函数不包含用于设置值的参数，则添加一个。
查找所有 setter 调用。
如果 setter 调用位于对当前类的构造函数的调用之后，则将其参数移动到构造函数调用并删除 setter。
将构造函数中的 setter 调用替换为对字段的直接访问。
删除 setter。

 */
}

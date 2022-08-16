package com.example.organizing_data;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:21
 * @Description TODO 将数组替换为每个元素都具有单独字段的对象
 */
public class Replace_Array_with_Object {
    /*
    问题
您有一个包含各种类型数据的数组。

解决方案
将数组替换为每个元素都具有单独字段的对象。

为什么要重构
数组是存储单一类型数据和集合的绝佳工具。但是如果你使用一个像邮政信箱这样的数组，将用户名存储在 box 1 中，将用户的地址存储在 box 14 中，你总有一天会非常不高兴。当有人将某些东西放入错误的“盒子”时，这种方法会导致灾难性的失败，并且还需要您花时间弄清楚哪些数据存储在哪里。

好处
在生成的类中，您可以放置​​以前存储在主类或其他地方的所有相关行为。
类的字段比数组的元素更容易记录。

如何重构
创建将包含来自数组的数据的新类。将数组本身作为公共字段放在类中。
创建一个字段，用于在原始类中存储该类的对象。不要忘记在您启动数据数组的位置创建对象本身。
在新类中，为每个数组元素一个一个地创建访问方法。给他们一个不言自明的名字，表明他们做了什么。同时，将主代码中每个数组元素的使用替换为对应的访问方式。
为所有元素创建访问方法后，将数组设为私有。
对于数组的每个元素，在类中创建一个私有字段，然后更改访问方法，以便他们使用此字段而不是数组。
移动完所有数据后，删除数组。

     */

    void before(){
        String[] row = new String[2];
        row[0] = "Liverpool";
        row[1] = "15";
    }

    /**
     * 替换为对象
     */
    void after(){
        Performance row = new Performance();
        row.setName("Liverpool");
        row.setWins("15");
    }

}
class Performance{
    private String name;
    private String wins;

    public void setName(String name) {
        this.name = name;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }
}
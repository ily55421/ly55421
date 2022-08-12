package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:22
 * @Description TODO Composite02 design pattern
 * Create a "lowest common denominator" that makes classes interchangeable
 * All concrete classes declare an "isa" relationship to the interface
 * All "container" classes couple themselves to the interface
 * "Container" classes use polymorphism as they delegate to their children
 * 复合设计模式(组合)
 *
 * 创建一个使类可互换的“最小公分母”
 *
 * 所有具体类都声明与接口的“isa”关系
 *
 * 所有“容器”类都与接口耦合
 *
 * “容器”类在委托给子代时使用多态性
 */
public class Composite_in_java02 {
    public static void main( String[] args ) {
        Composite02 first  = new Row( 1 );
        Composite02 second = new Column( 2 );
        Composite02 third  = new Column( 3 );
        Composite02 fourth = new Row( 4 );
        Composite02 fifth  = new Row( 5 );
        first.add(second);
        first.add(third);
        third.add(fourth);
        third.add(fifth);
        first.add(new Primitive(6));
        second.add(new Primitive(7));
        third.add(new Primitive(8));
        fourth.add(new Primitive(9));
        fifth.add(new Primitive(10));
        first.traverse();

        // 层级遍历
        //first(Row1  second(Col2  7)  third(Col3  fourth(Row4  9)  fifth(Row5  10)  8)  6)
    }
}
// 1. "lowest common denominator"
interface Component02 {
    void traverse();
}

// 2. "Isa" relationship
class Primitive implements Component02 {
    private int value;

    public Primitive(int val) {
        value = val;
    }

    public void traverse() {
        System.out.print( value + "  " );
    }
}

// 2. "Isa" relationship  是什么 的关系   基类
abstract class Composite02 implements Component02 {
    // 3. Couple to interface  耦合到接口
    private Component02[] children = new Component02[9];
    private int total = 0;
    private int value;
    public Composite02(int val) {
        value = val;
    }

    // 3. Couple to interface
    public void add(Component02 c) {
        children[total++] = c;
    }

    public void traverse() {
        System.out.print(value + "  ");
        for (int i=0; i < total; i++) {
            // 4. Delegation and polymorphism  委托和多态
            children[i].traverse();
        }
    }
}

// Two different kinds of "container" classes.  Most of the
// "meat" is in the Composite02 base class.
class Row extends Composite02 {
    public Row(int val) {
        super(val);
    }

    public void traverse() {
        System.out.print("Row");
        super.traverse();
    }
}

class Column extends Composite02 {
    public Column(int val) {
        super(val);
    }

    public void traverse() {
        System.out.print("Col");
        super.traverse();
    }
}


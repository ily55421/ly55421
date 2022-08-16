package com.design_patterns.creational_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 15:29
 * @Description TODO Abstract Factory classes are often implemented with Factory Methods, but they can also be implemented using Prototype. Abstract Factory might store a set of Prototypes from which to clone and return product objects.
 * <p>
 * Factory Method: creation through inheritance.
 * Prototype: creation through delegation.
 * Virtual constructor: defer choice of object to create until run-time.
 * <p>
 * 抽象工厂类通常使用工厂方法实现，但也可以使用原型实现。抽象工厂可能存储一组原型，从中克隆和返回产品对象。
 * <p>
 * 工厂方法：通过继承创建。
 * <p>
 * 原型：通过委托创建。
 * <p>
 * 虚拟构造函数：将要创建的对象的选择推迟到运行时。
 */
public class Abstract_Factory02 {
    public static void main(String[] args) {
        AbstractFactory02 factory;
//        args = new String[]{"ceshi"};  测试不同的生产工厂

        // 选择不同的工厂实例
        if (args.length > 0) {
            factory = new PCFactory();
        } else {
            factory = new NotPCFactory();
        }
        for (int i = 0; i < 3; i++) {
            System.out.print(factory.makePhase() + " ");
        }
        System.out.println();
        System.out.println(factory.makeCompromise());
        System.out.println(factory.makeGrade());
        //"short" "lie" "old"
        //"take test, deal with the results"
        //"my way, or the highway"

        //args = new String[]{"ceshi"};

        //"vertically challenged" "factually inaccurate" "chronologically gifted"
        //"do it your way, any way, or no way"
        //"you pass, self-esteem intact"
    }
}

/**
 * 一个类实现了Cloneable接口，以向Object.clone()方法指示该方法可以合法地对该类的实例进行逐个字段的复制。
 */
class Expression implements Cloneable {
    public String str;

    public Expression(String str) {
        this.str = str;
    }

    @Override
    public Expression clone() {
        Expression clone = null;
        try {
            clone = (Expression) super.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
        return clone;
    }

    @Override
    public String toString() {
        return str;
    }
}

abstract class AbstractFactory02 {
    public Expression prototype;

    /**
     * 克隆表达式对象
     *
     * @return
     */
    public Expression makePhase() {
        return prototype.clone();
    }

    public abstract Expression makeCompromise();

    public abstract Expression makeGrade();
}

class PCFactory extends AbstractFactory02 {

    public PCFactory() {
        prototype = new PCPhase();
    }

    @Override
    public Expression makeCompromise() {
        return new Expression("\"do it your way, any way, or no way\"");
    }

    @Override
    public Expression makeGrade() {
        return new Expression("\"you pass, self-esteem intact\"");
    }
}

class PCPhase extends Expression {
    private static int next = 0;
    private static final String[] list = {"\"animal companion\"", "\"vertically challenged\"",
            "\"factually inaccurate\"", "\"chronologically gifted\""};

    public PCPhase() {
        super(list[next]);
        next = (next + 1) % list.length;
    }

    @Override
    public Expression clone() {
        return new PCPhase();
    }
}

class NotPCPhase extends Expression {
    private static int next = 0;
    private static final String[] list = {"\"pet\"", "\"short\"", "\"lie\"", "\"old\""};

    public NotPCPhase() {
        super(list[next]);
        next = (next + 1) % list.length;
    }

    @Override
    public Expression clone() {
        return new NotPCPhase();
    }
}

class NotPCFactory extends AbstractFactory02 {

    public NotPCFactory() {
        prototype = new NotPCPhase();
    }

    @Override
    public Expression makeGrade() {
        return new Expression("\"my way, or the highway\"");
    }

    @Override
    public Expression makeCompromise() {
        return new Expression("\"take test, deal with the results\"");
    }
}

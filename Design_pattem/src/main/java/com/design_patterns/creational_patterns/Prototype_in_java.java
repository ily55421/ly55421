package com.design_patterns.creational_patterns;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: linK
 * @Date: 2022/8/12 16:55
 * @Description TODO Abstract Factory might store a set of Prototypes from which to clone and return product objects.
 * 抽象工厂可能存储一组原型，从中克隆和返回产品对象。
 */
public class Prototype_in_java {
    public static void main(String[] args) {
        //  初始化参数
        args = new String[]{"harry", "dick", "tom", "jack"};
        if (args.length > 0) {
            for (String type : args) {
                Person prototype = Factory.getPrototype(type);
                if (prototype != null) {
                    System.out.println(prototype);
                }
                //Harry
                //Dick
                //Tom
                //Prototype with name: jack, doesn't exist
            }
        } else {
            System.out.println("Run again with arguments of command string ");
        }
    }
}

interface Person {
    Person clone();
}

class Tom implements Person {
    private final String NAME = "Tom";

    @Override
    public Person clone() {
        return new Tom();
    }

    @Override
    public String toString() {
        return NAME;
    }
}

class Dick implements Person {
    private final String NAME = "Dick";

    @Override
    public Person clone() {
        return new Dick();
    }

    @Override
    public String toString() {
        return NAME;
    }
}

class Harry implements Person {
    private final String NAME = "Harry";

    @Override
    public Person clone() {
        return new Harry();
    }

    @Override
    public String toString() {
        return NAME;
    }
}

class Factory {
    /**
     * 存储原型类型
     */
    private static final Map<String, Person> prototypes = new HashMap<>();

    static {
        prototypes.put("tom", new Tom());
        prototypes.put("dick", new Dick());
        prototypes.put("harry", new Harry());
    }

    public static Person getPrototype(String type) {
        try {
            //返回对象 的 克隆对象  每次new 一个新的实例
            return prototypes.get(type).clone();
        } catch (NullPointerException ex) {
            System.out.println("Prototype with name: " + type + ", doesn't exist");
            return null;
        }
    }
}

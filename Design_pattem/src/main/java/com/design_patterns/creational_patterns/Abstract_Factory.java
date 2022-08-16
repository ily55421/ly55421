package com.design_patterns.creational_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 15:11
 * @Description TODO Abstract Factory classes are often implemented with Factory Methods, but they can also be implemented using Prototype. Abstract Factory might store a set of Prototypes from which to clone and return product objects.
 *
 * Factory Method: creation through inheritance.
 * Prototype: creation through delegation.
 * Virtual constructor: defer choice of object to create until run-time.
 *
 * 抽象工厂类通常使用工厂方法实现，但也可以使用原型实现。抽象工厂可能存储一组原型，从中克隆和返回产品对象。
 *
 * 工厂方法：通过继承创建。
 *
 * 原型：通过委托创建。
 *
 * 虚拟构造函数：将要创建的对象的选择推迟到运行时。
 */
public class Abstract_Factory {
    public static void main(String[] args) {
        // EMBER 生成具体的工厂类型
        AbstractFactory factory = AbstractFactory.getFactory(Architecture.EMBER);
        CPU cpu = factory.createCPU();
        System.out.println(cpu);
        //com.design_patterns.creational_patterns.EmberCPU@1540e19d
    }
}

// class CPU
abstract class CPU {
}

// class EmberCPU
class EmberCPU extends CPU {
}

// class EnginolaCPU
class EnginolaCPU extends CPU {
}

// class MMU
abstract class MMU {
}

// class EmberMMU
class EmberMMU extends MMU {
}

// class EnginolaMMU
class EnginolaMMU extends MMU {
}

// class EmberFactory
class EmberToolkit extends AbstractFactory {
    @Override
    public CPU createCPU() {
        return new EmberCPU();
    }

    @Override
    public MMU createMMU() {
        return new EmberMMU();
    }
}

// class EnginolaFactory
class EnginolaToolkit extends AbstractFactory {
    @Override
    public CPU createCPU() {
        return new EnginolaCPU();
    }

    @Override
    public MMU createMMU() {
        return new EnginolaMMU();
    }
}

enum Architecture {
    ENGINOLA, EMBER
}

abstract class AbstractFactory {
    private static final EmberToolkit EMBER_TOOLKIT = new EmberToolkit();
    private static final EnginolaToolkit ENGINOLA_TOOLKIT = new EnginolaToolkit();

    // Returns a concrete factory object that is an instance of the
    // concrete factory class appropriate for the given architecture.
    static AbstractFactory getFactory(Architecture architecture) {
        AbstractFactory factory = null;
        switch (architecture) {
            case ENGINOLA:
                factory = ENGINOLA_TOOLKIT;
                break;
            case EMBER:
                factory = EMBER_TOOLKIT;
                break;
        }
        return factory;
    }

    public abstract CPU createCPU();

    public abstract MMU createMMU();
}


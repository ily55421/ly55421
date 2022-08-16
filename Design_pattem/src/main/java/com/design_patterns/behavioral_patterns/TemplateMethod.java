package com.design_patterns.behavioral_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 10:24
 * @Description TODO
 */
public class TemplateMethod {
    public static void main(String[] args) {
        Generalization algorithm = new Realization();
        algorithm.findSolution();
        // 多重继承
        //Generalization.stepOne   默认方法
        //Realization.stepTwo   优先执行当前子类类方法
        //Specialization.step3_1  base类的方法
        //Realization.step3_2   覆写的 Specialization方法
        //Specialization.step3_3
        //Realization.stepFor
        //Generalization.stepFor
    }
}

abstract class Generalization {
    // 1. Standardize the skeleton of an algorithm in a "template" method
    //在“模板”方法中标准化算法的骨架
    void findSolution() {
        stepOne();
        stepTwo();
        stepThr();
        stepFor();
    }
    // 2. Common implementations of individual steps are defined in base class
    // 各个步骤的通用实现在基类中定义
    private void stepOne() {
        System.out.println("Generalization.stepOne");
    }
    // 3. Steps requiring peculiar implementations are "placeholders" in the base class
    // 需要特殊实现的步骤是基类中的“占位符”   留给子类实现
    abstract void stepTwo();
    abstract void stepThr();

    void stepFor() {
        System.out.println( "Generalization.stepFor" );
    }
}

abstract class Specialization extends Generalization {
    // 4. Derived classes can override placeholder methods
    // 派生类可以覆盖占位符方法
    // 1. Standardize the skeleton of an algorithm in a "template" method
    // 在“模板”方法中标准化算法的骨架
    protected void stepThr() {
        step3_1();
        step3_2();
        step3_3();
    }
    // 2. Common implementations of individual steps are defined in base class
    // 各个步骤的通用实现在基类中定义
    private void step3_1() {
        System.out.println("Specialization.step3_1");
    }

    // 3. Steps requiring peculiar implementations are "placeholders" in the base class
    // 需要特殊实现的步骤是基类中的“占位符”
    abstract protected void step3_2();

    private void step3_3() {
        System.out.println("Specialization.step3_3");
    }
}

class Realization extends Specialization {
    // 4. Derived classes can override placeholder methods
    // 派生类可以覆盖占位符方法
    protected void stepTwo() {
        System.out.println("Realization.stepTwo");
    }

    protected void step3_2() {
        System.out.println( "Realization.step3_2");
    }

    // 5. Derived classes can override implemented methods
    // 6. Derived classes can override and "call back to" base class methods
    // 派生类可以覆盖实现的方法
    // 派生类可以覆盖和“回调”基类方法
    protected void stepFor() {

        System.out.println("Realization.stepFor");
        super.stepFor();
    }
}


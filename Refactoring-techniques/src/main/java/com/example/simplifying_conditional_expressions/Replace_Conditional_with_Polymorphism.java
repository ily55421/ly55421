package com.example.simplifying_conditional_expressions;

/**
 * @Author: linK
 * @Date: 2022/8/15 16:08
 * @Description TODO  用多态替换条件表达式
 */
public class Replace_Conditional_with_Polymorphism {
    /*
    问题
您有一个根据对象类型或属性执行各种操作的条件。

解决方案
创建与条件分支匹配的子类。在其中，创建一个共享方法并将代码从条件的相应分支移动到它。然后用相关的方法调用替换条件。结果是根据对象类通过多态实现正确的实现。

为什么要重构
如果您的代码包含执行各种任务的运算符，这种重构技术会有所帮助，这些任务基于：
它实现的对象或接口的类
对象字段的值
调用对象方法之一的结果
如果出现新的对象属性或类型，您将需要在所有类似条件中搜索并添加代码。因此，如果有多个条件分散在对象的所有方法中，那么这种技术的好处就会成倍增加。

好处
这种技术遵循 Tell-Don't-Ask 原则：与其向对象询问其状态然后基于此执行操作，不如简单地告诉对象它需要做什么并让它自己决定要容易得多怎么做。
删除重复代码。你摆脱了许多几乎相同的条件。
如果你需要添加一个新的执行变体，你需要做的就是添加一个新的子类而不触及现有的代码（开放/封闭原则）。

如何重构
准备重构
对于这种重构技术，您应该有一个现成的包含替代行为的类层次结构。如果您没有这样的层次结构，请创建一个。其他技术将有助于实现这一目标：
用子类替换类型代码。将为特定对象属性的所有值创建子类。这种方法简单但不太灵活，因为您不能为对象的其他属性创建子类。
用状态/策略替换类型代码。一个类将专用于特定的对象属性，并且将从它为属性的每个值创建子类。当前类将包含对该类型对象的引用并将执行委托给它们。
以下步骤假定您已经创建了层次结构。

重构步骤
如果条件也在执行其他操作的方法中，请执行提取方法。
对于每个层次子类，重新定义包含条件的方法，并将相应条件分支的代码复制到该位置。
从条件中删除此分支。
重复替换，直到条件为空。然后删除条件并声明方法抽象。

     */
}
class Replace_Conditional_with_Polymorphism_Before{
    private double numberOfCoconuts;
   private double voltage;
    private boolean isNailed;
    protected
    // ...
    double getSpeed(test type) {
        switch (type) {
            case EUROPEAN:
                return getBaseSpeed();
            case AFRICAN:
                return getBaseSpeed() - getLoadFactor() * numberOfCoconuts;
            case NORWEGIAN_BLUE:
                return (isNailed) ? 0 : getBaseSpeed(voltage);
            default:
                throw new RuntimeException("Should be unreachable");
        }
    }

    private double getLoadFactor() {
        return 0;
    }
    private double getBaseSpeed() {
        return 0;
    }
    private double getBaseSpeed(double base) {
        return base*1;
    }
}


class Replace_Conditional_with_Polymorphism_After{
    abstract class Bird {
        // ...
        abstract double getSpeed();
    }

    class European extends Bird {
        double getSpeed() {
            return getBaseSpeed();
        }
     
    }
    class African extends Bird {
        private double numberOfCoconuts;
        
        double getSpeed() {
            return getBaseSpeed() - getLoadFactor() * numberOfCoconuts;
            
        }
  
       
    }
    class NorwegianBlue extends Bird {
        private boolean isNailed;
        private double voltage;
        double getSpeed() {
            return (isNailed) ? 0 : getBaseSpeed(voltage);
        }
    
    }

    // Somewhere in client code
    public void main(String[] args) {
        NorwegianBlue bird = new NorwegianBlue();
        double speed = bird.getSpeed();

    }

    private double getBaseSpeed(double base) {
        return base*1;
    }
    private double getBaseSpeed() {
        return 0;
    }
    private double getLoadFactor() {
        return 0;
    }

}



enum test{
    EUROPEAN,AFRICAN,NORWEGIAN_BLUE
}
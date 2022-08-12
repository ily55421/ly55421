package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/10 8:52
 * @Description TODO Strategy design pattern 策略设计模式
 * 1. Define the interface of an interchangeable family of algorithms 定义可互换算法族的接口
 * 2. Bury algorithm implementation details in derived classes  在派生类中隐藏算法实现细节
 * 3. Derived classes could be implemented using the Template Method pattern 派生类可以使用模板方法模式来实现
 * 4. Clients of the algorithm couple themselves strictly to the interface 算法的客户端将自己严格耦合到接口
 */

public class StrategyDemo {

    // 4. Clients couple strictly to the interface
    // client code here
    private static void execute(Strategy strategy) {
        strategy.solve();
    }

    public static void main(String[] args) {
        Strategy[] algorithms = {new FOO(), new BAR()};
        for (Strategy algorithm : algorithms) {
            execute(algorithm);
        }
        //Start  NextTry-1  IsSolution-false  NextTry-2  IsSolution-true  Stop
        //PreProcess  Search-1  PostProcess  PreProcess  Search-2
    }
}

// 1. Define the interface of the algorithm
interface Strategy {
    void solve();
}

// 2. Bury implementation
@SuppressWarnings("ALL")
abstract class StrategySolution implements Strategy {
    // 3. Template Method
    public void solve() {
        start();
        while (nextTry() && !isSolution()) {
        }
        stop();
    }

    abstract void start();

    abstract boolean nextTry();

    abstract boolean isSolution();

    abstract void stop();
}

/**
 * 方法具体实现
 */
class FOO extends StrategySolution {
    private int state = 1;

    protected void start() {
        System.out.print("Start  ");
    }

    protected void stop() {
        System.out.println("Stop");
    }

    protected boolean nextTry() {
        System.out.print("NextTry-" + state++ + "  ");
        return true;
    }

    protected boolean isSolution() {
        System.out.print("IsSolution-" + (state == 3) + "  ");
        return (state == 3);
    }
}

// 2. Bury implementation
abstract class StrategySearch implements Strategy {
    // 3. Template Method
    public void solve() {
        while (true) {
            preProcess();
            if (search()) {
                break;
            }
            postProcess();
        }
    }

    abstract void preProcess();

    abstract boolean search();

    abstract void postProcess();
}

@SuppressWarnings("ALL")
class BAR extends StrategySearch {
    private int state = 1;

    protected void preProcess() {
        System.out.print("PreProcess  ");
    }

    protected void postProcess() {
        System.out.print("PostProcess  ");
    }

    protected boolean search() {
        System.out.print("Search-" + state++ + "  ");
        return state == 3 ? true : false;
    }
}
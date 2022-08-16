package com.design_patterns.creational_patterns;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/12 17:03
 * @Description TODO   原型模块的实例初始化 ==> 更改为客户端调用时初始化原型类型数据
 */
public class Prototype_in_java02 {
    public static void main(String[] args) {
        //  初始化参数
        args = new String[]{"harry", "dick", "tom", "jack"};

        if (args.length > 0) {
            initializePrototypes();
            List<Prototype> prototypes = new ArrayList<>();
            // 6. Client does not use "new" 客户不使用 new 的方式创建
            for (String protoName : args) {
                // 获取原型对象
                Prototype prototype = PrototypeModule.createPrototype(protoName);
                // 添加进原型map
                if (prototype != null) {
                    prototypes.add(prototype);
                }
            }
            // 执行
            for (Prototype p : prototypes) {
                p.execute();
            }
        } else {
            System.out.println("Run again with arguments of command string ");
        }
    }

    // 3. Populate the "registry"  填充注册数据
    public static void initializePrototypes() {
        PrototypeModule.addPrototype(new PrototypeAlpha());
        PrototypeModule.addPrototype(new PrototypeBeta());
        PrototypeModule.addPrototype(new ReleasePrototype());
    }
}
// 1. The clone() contract
interface Prototype {
    Prototype clone();
    String getName();
    void execute();
}

/**
 * 原型模块
 */
class PrototypeModule {
    // 2. "registry" of prototypical objs  注册对象
    private static List<Prototype> prototypes = new ArrayList<>();

    // Adds a feature to the Prototype attribute of the PrototypesModule class
    // obj  The feature to be added to the Prototype attribute
    public static void addPrototype(Prototype p) {
        prototypes.add(p);
    }

    public static Prototype createPrototype(String name) {
        // 4. The "virtual ctor"
        for (Prototype p : prototypes) {
            if (p.getName().equals(name)) {
                return p.clone();
            }
        }
        System.out.println(name + ": doesn't exist");
        return null;
    }
}

// 5. Sign-up for the clone() contract.
// Each class calls "new" on itself FOR the client.
class PrototypeAlpha implements Prototype {
    private String name = "AlphaVersion";

    @Override
    public Prototype clone() {
        return new PrototypeAlpha();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() {
        System.out.println(name + ": does something");
    }
}

class PrototypeBeta implements Prototype {
    private String name = "BetaVersion";

    @Override
    public Prototype clone() {
        return new PrototypeBeta();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() {
        System.out.println(name + ": does something");
    }
}

class ReleasePrototype implements Prototype {
    private String name = "ReleaseCandidate";
    @Override
    public Prototype clone() {
        return new ReleasePrototype();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() {
        System.out.println(name + ": does real work");
    }
}


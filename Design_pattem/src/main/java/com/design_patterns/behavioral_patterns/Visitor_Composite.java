package com.design_patterns.behavioral_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 10:45
 * @Description TODO
 */
public class Visitor_Composite {
    public static void main(String[] args) {
        Base objects[] = {new FOO02(), new BAR02(), new BAZ()};
        for (Base object : objects) {
            for (int j = 0; j < 3; j++) {
                object.execute(objects[j]);
            }
            System.out.println();
        }
        //FOO02 object calls by yourself
        //FOO02 object was called from BAR02
        //FOO02 object was called from BAZ
        //
        //BAR02 object was called from FOO02
        //BAR02 object calls by yourself
        //BAR02 object was called from BAZ
        //
        //BAZ object was called from FOO02
        //BAZ object was called from BAR02
        //BAZ object calls by yourself
    }
}
interface Base {
    void execute(Base target);
    void doJob(FOO02 target);
    void doJob(BAR02 target);
    void doJob(BAZ target);
}

class FOO02 implements Base {
    public void execute(Base base) {
        base.doJob(this);
    }

    public void doJob(FOO02 foo) {
        System.out.println("FOO02 object calls by yourself");
    }

    public void doJob(BAR02 bar) {
        System.out.println("BAR02 object was called from FOO02");
    }

    public void doJob(BAZ baz) {
        System.out.println("BAZ object was called from FOO02");
    }
}

class BAR02 implements Base {
    public void execute(Base base) {
        base.doJob(this);
    }

    public void doJob(FOO02 foo) {
        System.out.println("FOO02 object was called from BAR02" );
    }

    public void doJob(BAR02 bar) {
        System.out.println("BAR02 object calls by yourself");
    }

    public void doJob(BAZ baz) {
        System.out.println("BAZ object was called from BAR02");
    }
}

class BAZ implements Base {
    public void execute(Base base) {
        // 回调
        base.doJob( this);
    }

    public void doJob(FOO02 foo) {
        System.out.println("FOO02 object was called from BAZ");
    }

    public void doJob(BAR02 bar) {
        System.out.println("BAR02 object was called from BAZ");
    }

    public void doJob(BAZ baz) {
        System.out.println("BAZ object calls by yourself");
    }
}

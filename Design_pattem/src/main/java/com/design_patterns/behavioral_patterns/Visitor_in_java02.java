package com.design_patterns.behavioral_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 11:22
 * @Description TODO
 */
public class Visitor_in_java02 {
    public static void main( String[] args ) {
        Element02[] list = {new FOO03(), new BAR03(), new BAZ02()};
        UpVisitor02 up = new UpVisitor02();
        DownVisitor02 down = new DownVisitor02();
        for (Element02 element : list) {
            element.accept(up);
        }
        for (Element02 element : list) {
            element.accept(down);
        }
        //do Up on FOO03
        //do Up on BAR03
        //do Up on BAZ02
        //do Down on FOO03
        //do Down on BAR03
        //do Down on BAZ02
    }
}
interface Element02 {
    void accept(Visitor v);
}

/**
 * 元素实现
 */
class FOO03 implements Element02 {
    public void accept(Visitor v) {
        v.visit(this);
    }

    public String getFOO03() {
        return "FOO03";
    }
}

class BAR03 implements Element02 {
    public void   accept( Visitor v ) {
        v.visit( this );
    }

    public String getBAR03() {
        return "BAR03";
    }
}

class BAZ02 implements Element02 {
    public void accept(Visitor v) {
        v.visit(this);
    }

    public String getBAZ02() {
        return "BAZ02";
    }
}

/**
 * 访问接口
 * 如果需要添加 可访问对象 Visitable  就要追加一个访问接口 然后去具体的访问者中实现
 *
 * {@link Visitor_Double_dispatch 利用反射的实现}
 */
interface Visitor {
    void visit(FOO03 foo);
    void visit(BAR03 bar);
    void visit(BAZ02 baz);
}

/**
 * 具体的访问对象
 */
class UpVisitor02 implements Visitor {
    public void visit(FOO03 foo) {
        System.out.println("do Up on " + foo.getFOO03());
    }

    public void visit(BAR03 bar) {
        System.out.println("do Up on " + bar.getBAR03());
    }

    public void visit(BAZ02 baz) {
        System.out.println( "do Up on " + baz.getBAZ02() );
    }
}

class DownVisitor02 implements Visitor {
    public void visit(FOO03 foo) {
        System.out.println("do Down on " + foo.getFOO03());
    }

    public void visit(BAR03 bar) {
        System.out.println("do Down on " + bar.getBAR03());
    }

    public void visit(BAZ02 baz ) {
        System.out.println("do Down on " + baz.getBAZ02());
    }
}


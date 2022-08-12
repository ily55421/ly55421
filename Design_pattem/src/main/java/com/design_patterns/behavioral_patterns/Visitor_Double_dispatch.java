package com.design_patterns;

import java.lang.reflect.Method;

/**
 * @Author: linK
 * @Date: 2022/8/11 10:55
 * @Description TODO 双重派送
 * Problem."If you want to add a new Visitable object,
 * you have to change the Visitor interface,
 * and then implement that method in each of your Visitors."
 * 如果你想添加一个新的 Visitable 对象，你必须改变 Visitor 接口，然后在你的每个 Visitor 中实现该方法。
 * Solution. With the ReflectiveVisitor,
 * you only need one method in the Visitor interface - visit(Object).
 * All other visit() methods can be added later as point-to-point coupling is required.
 * 使用ReflectiveVisitor，您只需要Visitor 接口中的一种方法——visit(Object)。
 * 所有其他 visit() 方法可以稍后添加，因为需要点对点耦合
 */
public class Visitor_Double_dispatch {
    public static void main(String[] args) {
        Element[] list = {new This(), new That(), new TheOther()};
        // 向上访问
        UpVisitor up = new UpVisitor();
        // 向下访问
        DownVisitor down = new DownVisitor();
        for (Element element : list) {
            element.accept(up);
        }
        for (Element element : list) {
            element.accept(down);
        }
        //UpVisitor: do Up on This
        //UpVisitor: generic visitObject() method
        //ReflectiveVisitor: do Base on TheOther
        //DownVisitor - no appropriate visit() method
        //DownVisitor: do Down on That
        //ReflectiveVisitor: do Base on TheOther
        This aThis = new This();
        aThis.accept(up);
        //客户端创建“访问者”对象并将每个对象传递给 accept() 调用
        //UpVisitor: do Up on This   通过反射的方法作为入参 调用
    }
}

// The "element" hierarchy  元素层
interface Element {
    void accept(ReflectiveVisitor v);
}

class This implements Element {
    public void accept(ReflectiveVisitor v) {
        v.visit(this);
    }

    public String thiss() {
        return "This";
    }
}

class That implements Element {
    public void accept(ReflectiveVisitor v) {
        v.visit(this);
    }

    public String that() {
        return "That";
    }
}

class TheOther implements Element {
    public void accept(ReflectiveVisitor v) {
        v.visit(this);
    }

    public String theOther() {
        return "TheOther";
    }
}

/**
 * 反射访问者
 * 操作层
 * 通过反射调用 执行不同的访问对象方法
 */
// The "operation" hierarchy
abstract class ReflectiveVisitor {
    abstract public void visit(Object o);

    public void visitTheOther(TheOther e) {
        System.out.println("ReflectiveVisitor: do Base on " + e.theOther());
    }

    // 1. Look for visitElementClassName() in the current class
    // 2. Look for visitElementClassName() in superclasses
    // 3. Look for visitElementClassName() in interfaces
    // 4. Look for visitObject() in current class
    protected Method getMethod(Class source) {
        // 接收参数 不直接对原参数进行操作
        Class clazz = source;
        Method methodName = null;
        // methodName == null && clazz != Object.class 边界判断
        while (methodName == null && clazz != Object.class) {
            String clazzName = clazz.getName();
            clazzName = "visit" + clazzName.substring(clazzName.lastIndexOf('.') + 1);
            try {

                // 获取方法对象
                methodName = getClass().getMethod(clazzName, clazz);
            } catch (NoSuchMethodException ex) {
                // 向上查找
                clazz = clazz.getSuperclass();
            }
        }
        // 如果为顶级对象  一直查找到最上级 都没有找到对应的方法
        if (clazz == Object.class) {
            // System.out.println( "Searching for interfaces" );
            Class[] interfaces = source.getInterfaces();
            for (Class intface : interfaces) {
                String interfaceName = intface.getName();
                interfaceName = "visit" + interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
                try {
                    // 获取接口方法
                    methodName = getClass().getMethod(interfaceName, intface);
                } catch (NoSuchMethodException ex) {
                    //ex.printStackTrace();
                }
            }
        }
        // 如果方法名未找到   返回默认 当前访问对象方法
        if (methodName == null) {
            try {
                methodName = getClass().getMethod("visitObject", Object.class);
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
        return methodName;
    }
}

class UpVisitor extends ReflectiveVisitor {
    public void visit(Object o) {
        try {
            // this 当前对象  o 入参
            getMethod(o.getClass()).invoke(this, o);
        } catch (Exception ex) {
            System.out.println("UpVisitor - no appropriate visit() method");
        }
    }

    /**
     * 访问者This
     * @param element
     */
    public void visitThis(This element) {
        System.out.println("UpVisitor: do Up on " + element.thiss());
    }

    /**
     * 访问者为对象
     * @param o
     */
    public void visitObject(Object o) {
        System.out.println("UpVisitor: generic visitObject() method");
    }
}

class DownVisitor extends ReflectiveVisitor {
    public void visit(Object o) {
        try {
            getMethod(o.getClass()).invoke(this, o);
        } catch (Exception ex) {
            System.out.println("DownVisitor - no appropriate visit() method");
        }
    }

    /**
     * 访问者That
     * @param element
     */
    public void visitThat(That element) {
        System.out.println("DownVisitor: do Down on " + element.that());
    }
}

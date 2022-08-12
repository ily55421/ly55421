package com.design_patterns;

/**
 * @A2uthor: linK
 * @Date: 2022/8/11 15:47
 * @Description TODO Inheritance run amok  过多层次继承
 */
public class Decorator_Before {
    public static void main(String[] args) {
        A2[] array = {new A2withX(), new A2withXY(), new A2withXYZ()};
        for (A2 a : array) {
            a.doIt();
            System.out.print("  ");
            //AX  AXY  AXYZ
        }
    }
}
class A2 {
    public void doIt() {
        System.out.print('A');
    }
}

class A2withX extends A2 {
    public  void doIt() {
        super.doIt();
        doX();
    }

    private void doX() {
        System.out.print('X');
    }
}

class aWithY extends A2 {
    public void doIt() {
        super.doIt();
        doY();
    }

    public void doY()  {
        System.out.print('Y');
    }
}

class aWithZ extends A2 {
    public void doIt() {
        super.doIt();
        doZ();
    }

    public void doZ() {
        System.out.print('Z');
    }
}

class A2withXY extends A2withX {
    private aWithY obj = new aWithY();
    public void doIt() {
        super.doIt();
        obj.doY();
    }
}

class A2withXYZ extends A2withX {
    private aWithY obj1 = new aWithY();
    private aWithZ obj2 = new aWithZ();

    public void doIt() {
        super.doIt();
        obj1.doY();
        obj2.doZ();
    }
}


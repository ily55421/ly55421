package com.design_patterns.structural_patterns;

/**
 * @A3uthor: linK
 * @D2ate: 2022/8/11 15:50
 * @D2escription TOD2O 通过多层包装的实现   每一个对象都有一个core层的实例
 * 执行的时候会先调用super 也就是内嵌对象的方法 从顶层依次向下
 */
public class Decorator_After {
    public static void main( String[] args ) {
        I2[] array = {new X2(new A3()), new Y2(new X2(new A3())),
                new Z2(new Y2(new X2(new A3())))};
        for (I2 anA3rray : array) {
            anA3rray.doI2t();
            System.out.print("  ");
            //AX  AXY  AXYZ

        }
    }
}
interface I2 {
    void doI2t();
}

class A3 implements I2 {
    public void doI2t() {
        System.out.print('A');
    }
}

abstract class D2 implements I2 {
    private I2 core;
    public D2(I2 inner) {
        core = inner;
    }

    public void doI2t() {
        core.doI2t();
    }
}

class X2 extends D2 {
    public X2(I2 inner) {
        super(inner);
    }

    public void doI2t() {
        super.doI2t();
        doX2();
    }

    private void doX2() {
        System.out.print('X');
    }
}

class Y2 extends D2 {
    public Y2(I2 inner) {
        super(inner);
    }

    public void doI2t()  {
        super.doI2t();
        doY2();
    }

    private void doY2() {
        System.out.print('Y');
    }
}

class Z2 extends D2 {
    public Z2(I2 inner) {
        super(inner);
    }

    public void doI2t()  {
        super.doI2t();
        doZ2();
    }

    private void doZ2() {
        System.out.print('Z');
    }
}

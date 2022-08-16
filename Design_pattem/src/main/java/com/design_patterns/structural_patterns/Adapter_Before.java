package com.design_patterns.structural_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 14:25
 * @Description TODO Because the interface between Line and Rectangle objects is incompatible, the user has to recover the type of each shape and manually supply the correct arguments.
 * 由于 Line 和 Rectangle 对象之间的接口不兼容，用户必须恢复每个形状的类型并手动提供正确的参数。
 */
public class Adapter_Before {
    public static void main(String[] args) {
        Object[] shapes = {new Line(), new Rectangle()};
        int x1 = 10, y1 = 20;
        int x2 = 30, y2 = 60;
        int width = 40, height = 40;
        // 双重赋值
        //        x1 = x2 =y2;
        //        System.out.println(x1);
        for (Object shape : shapes) {
            if (shape.getClass().getSimpleName().equals("Line")) {
                ((Line) shape).draw(x1, y1, x2, y2);
            } else if (shape.getClass().getSimpleName().equals("Rectangle")) {
                ((Rectangle) shape).draw(x2, y2, width, height);
            }
        }
        //Line from point A(10;20), to point B(30;60)
        //Rectangle with coordinate left-down point (30;60), width: 40, height: 40
    }
}

class Line {
    public void draw(int x1, int y1, int x2, int y2) {
        System.out.println("Line from point A(" + x1 + ";" + y1 + "), to point B(" + x2 + ";" + y2 + ")");
    }
}

class Rectangle {
    public void draw(int x, int y, int width, int height) {
        System.out.println("Rectangle with coordinate left-down point (" + x + ";" + y + "), width: " + width
                + ", height: " + height);
    }
}


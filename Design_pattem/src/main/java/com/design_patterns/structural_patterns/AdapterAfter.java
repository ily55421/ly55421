package com.design_patterns.structural_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 14:36
 * @Description TODO The Adapter's "extra level of indirection" takes care of mapping a user-friendly common interface to legacy-specific peculiar interfaces.
 * 适配器的“额外间接层”负责将用户友好的通用接口映射到特定于传统的特殊接口。
 */
public class AdapterAfter {
    public static void main(String[] args) {
        Shape02[] shapes = {new Rectangle02Adapter(new Rectangle02()),
                new Line02Adapter(new Line02())};
        int x1 = 10, y1 = 20;
        int x2 = 30, y2 = 60;
        for (Shape02 shape : shapes) {
            shape.draw(x1, y1, x2, y2);
            // 调用对应子类实现
        }
        //Rectangle02 with coordinate left-down point (10;20), width: 20, height: 40
        //Line02 from point A(10;20), to point B(30;60)
    }
}

/**
 * 原规则接口
 */
interface Shape02 {
    void draw(int x, int y, int z, int j);
}

/**
 * 具体实现对象
 */
class Line02 {
    public void draw(int x1, int y1, int x2, int y2) {
        System.out.println("Line02 from point A(" + x1 + ";" + y1 + "), to point B(" + x2 + ";" + y2 + ")");
    }
}

class Rectangle02 {
    public void draw(int x, int y, int width, int height) {
        System.out.println("Rectangle02 with coordinate left-down point (" + x + ";" + y + "), width: " + width
                + ", height: " + height);
    }
}

/**
 * 包装类 对line进行包装
 */
class Line02Adapter implements Shape02 {
    private Line02 adaptee;

    public Line02Adapter(Line02 line) {
        this.adaptee = line;
    }

    @Override
    public void draw(int x1, int y1, int x2, int y2) {
        adaptee.draw(x1, y1, x2, y2);
    }
}

class Rectangle02Adapter implements Shape02 {
    private Rectangle02 adaptee;

    public Rectangle02Adapter(Rectangle02 rectangle) {
        this.adaptee = rectangle;
    }

    @Override
    public void draw(int x1, int y1, int x2, int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);
        adaptee.draw(x, y, width, height);
    }
}


package com.design.flyweightPattern;

import java.util.HashMap;

public class ShapeFactory {
    /**
     * localHashmap  共享变量对象
     */
    private static final HashMap<String, Shape> circleMap = new HashMap();

    public static Shape getCircle(String color) {
        Circle circle = (Circle) circleMap.get(color);
        // 判空生产对象
        if (circle == null) {
            circle = new Circle(color);
            circleMap.put(color, circle);
            System.out.println("Creating circle of color : " + color);
        }
        return circle;
    }
}
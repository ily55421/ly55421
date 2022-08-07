package com.design.facadePattern;

/**
 * 使用该外观类画出各种类型的形状
 */
public class FacadePatternDemo {
    public static void main(String[] args) {
        // 形状制造者  ShapeMaker 封装所有具体的形状
        ShapeMaker shapeMaker = new ShapeMaker();

        shapeMaker.drawCircle();
        shapeMaker.drawRectangle();
        shapeMaker.drawSquare();
    }
}
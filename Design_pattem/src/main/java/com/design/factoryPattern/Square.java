package com.design.factoryPattern;

/**
 * 不同的实现类
 */
public class Square implements Shape {

    @Override
    public void draw() {
        System.out.println("Inside Square::draw() method.");
    }
}
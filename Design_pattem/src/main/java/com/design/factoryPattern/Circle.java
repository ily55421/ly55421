package com.design.factoryPattern;

/**
 * 不同的实现类
 */
public class Circle implements Shape {

    @Override
    public void draw() {
        System.out.println("Inside Circle::draw() method.");
    }
}
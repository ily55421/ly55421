package com.design.visitorPattern;

/**
 * @author lin 2022/8/8 22:21
 */
public class VisitorPatternDemo {
    public static void main(String[] args) {

        ComputerPart computer = new Computer();
        //元素的执行算法可以随着访问者改变而改变 调用访问者对象执行方法
        computer.accept(new ComputerPartDisplayVisitor());
        //Displaying Mouse.
        //Displaying Keyboard.
        //Displaying Monitor.
        //Displaying Computer.
    }
}

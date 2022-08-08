package com.design.observerPattern;

/**
 * @author lin 2022/8/8 21:54
 */
public class ObserverPatternDemo {
    public static void main(String[] args) {
        Subject subject = new Subject();

        new HexaObserver(subject);
        new OctalObserver(subject);
        new BinaryObserver(subject);

        System.out.println("First state change: 15");
        subject.setState(15);
        System.out.println("Second state change: 10");
        subject.setState(10);
        //First state change: 15
        //Hex String: F
        //Octal String: 17
        //Binary String: 1111
        //Second state change: 10
        //Hex String: A
        //Octal String: 12
        //Binary String: 1010
    }
}
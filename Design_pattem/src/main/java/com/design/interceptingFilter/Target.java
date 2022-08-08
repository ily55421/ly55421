package com.design.interceptingFilter;

/**
 * @author lin 2022/8/9 1:02
 */
public class Target {
    public void execute(String request){
        System.out.println("Executing request: " + request);
    }
}

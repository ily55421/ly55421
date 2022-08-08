package com.design.interceptingFilter;

/**
 * @author lin 2022/8/9 1:02
 */
public class DebugFilter implements Filter {
    public void execute(String request){
        System.out.println("request log: " + request);
    }
}

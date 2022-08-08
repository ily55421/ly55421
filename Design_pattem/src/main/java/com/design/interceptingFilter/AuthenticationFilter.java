package com.design.interceptingFilter;

/**
 * @author lin 2022/8/9 1:01
 */
public class AuthenticationFilter implements Filter {
    public void execute(String request){
        System.out.println("Authenticating request: " + request);
    }
}

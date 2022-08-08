package com.design.frontControllerPattern;

/**
 * @author lin 2022/8/9 0:57
 */
public class FrontControllerPatternDemo {
    public static void main(String[] args) {
        FrontController frontController = new FrontController();

        //请求调度
        frontController.dispatchRequest("HOME");
        frontController.dispatchRequest("STUDENT");
        //Page requested: HOME
        //User is authenticated successfully.
        //Displaying Home Page
        //Page requested: STUDENT
        //User is authenticated successfully.
        //Displaying Student Page
    }
}

package com.design.interceptingFilter;

/**
 * @author lin 2022/8/9 1:03
 */
public class InterceptingFilterDemo {
    public static void main(String[] args) {
        // 添加过滤器链
        FilterManager filterManager = new FilterManager(new Target());
        filterManager.setFilter(new AuthenticationFilter());
        filterManager.setFilter(new DebugFilter());

        //客户测试调用
        Client client = new Client();
        client.setFilterManager(filterManager);
        client.sendRequest("HOME");
        //Authenticating request: HOME
        //request log: HOME
        //Executing request: HOME
    }
}

package com.design.businessDelegatePattern;

/**
 * @author lin 2022/8/8 22:30
 */

public class BusinessLookUp {
    /**
     * 工厂方法类 返回不同的实例对象
     *
     * @param serviceType
     * @return
     */
    public BusinessService getBusinessService(String serviceType) {
        if (serviceType.equalsIgnoreCase("EJB")) {
            return new EJBService();
        } else {
            return new JMSService();
        }
    }
}
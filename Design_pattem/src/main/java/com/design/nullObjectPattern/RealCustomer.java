package com.design.nullObjectPattern;

/**
 * @author lin 2022/8/8 22:02
 */

public class RealCustomer extends AbstractCustomer
{

    public RealCustomer(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isNil() {
        return false;
    }
}

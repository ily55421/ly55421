package com.design.nullObjectPattern;

/**
 * @author lin 2022/8/8 22:02
 */
public class NullCustomer extends AbstractCustomer {

    @Override
    public String getName() {
        return "Not Available in Customer Database";
    }

    @Override
    public boolean isNil() {
        return true;
    }
}
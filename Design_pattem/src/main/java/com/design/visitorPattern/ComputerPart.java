package com.design.visitorPattern;

/**
 * @author lin 2022/8/8 22:12
 */
public interface ComputerPart {
    public void accept(ComputerPartVisitor computerPartVisitor);
}

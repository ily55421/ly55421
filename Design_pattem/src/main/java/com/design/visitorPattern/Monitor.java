package com.design.visitorPattern;

/**
 * @author lin 2022/8/8 22:19
 */
public class Monitor  implements ComputerPart {

    @Override
    public void accept(ComputerPartVisitor computerPartVisitor) {
        computerPartVisitor.visit(this);
    }
}
package com.design.interpreterPattern;

public class InterpreterPatternDemo {

    //规则：Robert 和 John 是男性
    public static Expression getMaleExpression() {
        Expression robert = new TerminalExpression("Robert");
        Expression john = new TerminalExpression("John");
        return new OrExpression(robert, john);
    }

    //规则：Julie 是一个已婚的女性
    public static Expression getMarriedWomanExpression() {
        Expression julie = new TerminalExpression("Julie");
        Expression married = new TerminalExpression("Married");
        return new AndExpression(julie, married);
    }

    public static void main(String[] args) {
        Expression isMale = getMaleExpression();
        Expression isMarriedWoman = getMarriedWomanExpression();
        // isMale.interpret("John")  执行解析器 是否在可解析的范围内
        System.out.println("John is male? " + isMale.interpret("John"));
        System.out.println("Robert is male? " + isMale.interpret("Robert"));

        System.out.println("Julie is a married women? "
                + isMarriedWoman.interpret("Married Julie"));
        System.out.println("Married is a married women? "
                + isMarriedWoman.interpret(" Julie"));
        //John is male? true
        //Robert is male? true
        //Julie is a married women? true
        //Married is a married women? false
    }
}
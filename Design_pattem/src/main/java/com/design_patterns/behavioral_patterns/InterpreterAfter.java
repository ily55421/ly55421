package com.design_patterns.behavioral_patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static com.design_patterns.behavioral_patterns.InterpreterBefore.precedence;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:05
 * @Description TODO This is a refactoring that follows the intent of the Interpreter design pattern.
 * All classes in the Operand hierarchy: implement the evaluate(context), digest some piece of the context argument,
 * and return their contribution to the recursive traversal.
 * Applying the Interpreter pattern in this domain is probably inappropriate.
 * 这是一个遵循解释器设计模式意图的重构。操作数层次结构中的所有类：实现评估（上下文），消化一些上下文参数，并返回它们对递归遍历的贡献。
 * todo 在这个领域应用解释器模式可能是不合适的。
 *
 * 中缀后缀 之间切换 没看懂
 */
public class InterpreterAfter {
    public static InterpreterBefore interpreter
            = new InterpreterBefore();

    public static String convertToPostfix(String expr) {
        Stack<Character> operationsStack = new Stack<>();
        StringBuilder out = new StringBuilder();
        String operations = "+-*/()";
        char topSymbol = '+';
        boolean empty;
        String[] tokens = expr.split(" ");
        for (String token : tokens) {
            if (operations.indexOf(token.charAt(0)) == -1) {
                out.append(token);
                out.append(' ');
            } else {
                while (!(empty = operationsStack.isEmpty()) && precedence(topSymbol =
                        operationsStack.pop(), token.charAt(0))) {
                    out.append(topSymbol);
                    out.append(' ');
                }
                if (!empty) {
                    operationsStack.push(topSymbol);
                }
                if (empty || token.charAt(0) != ')') {
                    operationsStack.push(token.charAt(0));
                } else {
                    topSymbol = operationsStack.pop();
                }
            }
        }
        while (!operationsStack.isEmpty()) {
            out.append(operationsStack.pop());
            out.append(' ');
        }
        return out.toString();
    }

    public static Operand buildSyntaxTree(String tree) {
        Stack <Operand> stack = new Stack<>();
        String operations = "+-*/";
        String[] tokens = tree.split(" ");
        for (String token : tokens) {
            if (operations.indexOf(token.charAt(0)) == -1) {
                Operand term;
                try {
                    term = new Number(Double.parseDouble(token));
                } catch (NumberFormatException ex) {
                    term = new Variable(token);
                }
                stack.push(term);

                // If token is an operator
            } else {
                Expression expr = new Expression(token.charAt(0));
                expr.right = stack.pop();
                expr.left = stack.pop();
                stack.push(expr);
            }
        }
        return stack.pop();
    }

    public static void main(String[] args) {
        System.out.println("celsius * 9 / 5 + thirty");
        String postfix = convertToPostfix("celsius * 9 / 5 + thirty");
        System.out.println(postfix);
        Operand expr = buildSyntaxTree(postfix);
        expr.traverse(1);
        System.out.println();
        HashMap< String, Integer > map = new HashMap<>();
        map.put("thirty", 30);
        for (int i = 0; i <= 100; i += 10) {
            map.put("celsius", i);
            System.out.println("C is " + i + ",  F is " + expr.evaluate(map));
        }
    }
}
interface Operand {
    double evaluate(Map<String, Integer> context);
    void traverse(int level);
}

class Expression implements Operand {
    private char operation;

    public Operand left, right;

    public Expression(char operation) {
        this.operation = operation;
    }
    public void traverse(int level) {
        left.traverse(level + 1);
        System.out.print("" + level + operation + level + " ");
        right.traverse(level + 1);
    }

    public double evaluate(Map<String, Integer> context) {
        double result = 0;
        double a = left.evaluate(context);
        double b = right.evaluate(context);
        if (operation == '+') {
            result = a + b;
        }
        if (operation == '-') {
            result = a - b;
        }
        if (operation == '*') {
            result = a * b;
        }
        if (operation == '/') {
            result = a / b;
        }
        return result;
    }
}

class Variable implements Operand {
    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public void traverse(int level) {
        System.out.print(name + " ");
    }

    public double evaluate(Map<String, Integer> context) {
        return context.get(name);
    }
}

class Number implements Operand {
    private double value;

    public Number(double value) {
        this.value = value;
    }

    public void traverse(int level) {
        System.out.print(value + " ");
    }

    public double evaluate(Map context) {
        return value;
    }
}



package com.design_patterns.behavioral_patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @Author: linK
 * @Date: 2022/8/10 13:44
 * @Description TODO This is an adaptation of a design that appeared in a Pascal data structures book.
 * The intent was to use stacks to convert normal "infix" syntax into "postfix" notation with operator precedence already handled.
 * 这是对 Pascal 数据结构书中出现的设计的改编。其目的是使用堆栈将普通的“中缀”语法转换为“后缀”表示法，其中运算符优先级已经处理。
 * TODO 只能用于节点符号的解释 不能作为解析器使用
 */
public class InterpreterBefore {

    public static void main(String[] args) {
        String infix = "C * 2229 / 5 + 32";
        String postfix = convertToPostfix(infix);
        System.out.println("Infix:   " + infix);
        System.out.println("Postfix: " + postfix);
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i <= 100; i += 10) {
            map.put("C", i);
            System.out.println("C is " + i + ",  F is " + processPostfix(postfix, map));
        }
    }

    /**
     * 优先级
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean precedence(char a, char b) {
        String high = "*/", low = "+-";
        if (a == '(') {
            return false;
        }
        if (a == ')' && b == '(') {
            System.out.println(")-(");
            return false;
        }
        if (b == '(') {
            return false;
        }
        if (b == ')') {
            return true;
        }
        if (high.indexOf(a) > -1 && low.indexOf(b) > -1) {
            return true;
        }
        if (high.indexOf(a) > -1 && high.indexOf(b) > -1) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (low.indexOf(a) > -1 && low.indexOf(b) > -1) {
            return true;
        }
        return false;
    }

    /**
     * 转换成后缀表达式
     *
     * @param expr
     * @return
     */
    public static String convertToPostfix(String expr) {
        // 操作栈
        Stack<Character> operationsStack = new Stack<>();
        StringBuilder out = new StringBuilder();
        // 操作符
        String operations = "+-*/()";

        char topSymbol = '+';
        boolean empty;
        String[] tokens = expr.split(" ");
        for (String token : tokens) {
            // operations.indexOf(token.charAt(0)) 字符是否为空
            if (operations.indexOf(token.charAt(0)) == -1) {
                out.append(token);
                out.append(' ');
            } else {
                // 不为空且判断顶部操作等级
                while (!(empty = operationsStack.isEmpty()) &&
                        precedence(topSymbol = operationsStack.pop(), token.charAt(0))) {
                    out.append(topSymbol);
                    out.append(' ');
                }
                // 字符不为空 入栈
                if (!empty) {
                    operationsStack.push(topSymbol);
                }
                //
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

    public static double processPostfix(String postfix, Map<String, Integer> map) {
        Stack<Double> stack = new Stack<>();
        String operations = "+-*/";
        String[] tokens = postfix.split(" ");
        for (String token : tokens) {
            // If token is a number or variable    如果 token 是数字或变量
            if (operations.indexOf(token.charAt(0)) == -1) {
                double term;
                try {
                    term = Double.parseDouble(token);
                } catch (NumberFormatException ex) {
                    term = map.get(token);
                }
                stack.push(term);

                // If token is an operator  如果token 是操作符
            } else {
                double b = stack.pop(), a = stack.pop();
                if (token.charAt(0) == '+') {
                    a = a + b;
                }
                if (token.charAt(0) == '-') {
                    a = a - b;
                }
                if (token.charAt(0) == '*') {
                    a = a * b;
                }
                if (token.charAt(0) == '/') {
                    a = a / b;
                }
                stack.push(a);
            }
        }
        return stack.pop();
    }


}

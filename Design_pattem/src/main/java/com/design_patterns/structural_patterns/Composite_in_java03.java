package com.design_patterns.structural_patterns;

import java.util.ArrayList;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:32
 * @Description TODO Composite design pattern
 * A static attribute in the Entity03 base class can be manipulated by each Box03 object to control indentation.
 * 每个 Box03 对象都可以操作实体基类中的静态属性来控制缩进
 */
public class Composite_in_java03 {
    public static void main(String[] args) {
        // 入参 2 4 6
        Box03 root = initialize();
        int[] levels = new int[args.length];
        for (int i=0; i < args.length; i++) {
            levels[i] = Integer.parseInt(args[i]);
        }
        // 层次遍历
        root.traverse( levels );
        //21
        //   41
        //   42
        //      61
        //      62
        //      63
        //   43
        //22
        //   44
        //   45
        //      64
        //      65
        //      66
        //   46
        //23
        //   47
        //   48
        //      67
        //      68
        //      69
        //   49
    }

    private static Box03 initialize() {
        Box03[] nodes = new Box03[7];
        nodes[1] = new Box03( 1 );
        int[] waves = {1, 4, 7};
        for (int i=0; i < 3; i++) {
            nodes[2] = new Box03(21+i);
            nodes[1].add(nodes[2]);
            int level = 3;
            for (int j=0; j < 4; j++) {
                nodes[level-1].add( new Product03(level*10 + waves[i]));
                nodes[level] = new Box03(level*10 + waves[i]+1);
                nodes[level-1].add(nodes[level]);
                nodes[level-1].add(new Product03(level*10 + waves[i]+2));
                level++;
            }
        }
        return nodes[1];
    }
}
abstract class Entity03 {
    protected static StringBuffer indent = new StringBuffer();
    protected static int level = 1;

    public abstract void traverse(int[] levels);

    protected boolean printThisLevel(int[] levels) {
        for (int value : levels) {
            if (level == value) {
                return true;
            }
        }
        return false;
    }
}

class Product03 extends Entity03 {
    private int value;
    public Product03(int value) {
        this.value = value;
    }

    public void traverse(int[] levels) {
        if (printThisLevel(levels)) {
            System.out.println(indent.toString() + value);
        }
    }
}

class Box03 extends Entity03 {
    private  java.util.List children = new ArrayList();
    private int value;
    public Box03(int val) {
        value = val;
    }

    public void add(Entity03 c) {
        children.add(c);
    }

    public void traverse(int[] levels) {
        if (printThisLevel(levels)) {
            System.out.println(indent.toString() + value);
            indent.append( "   " );
        }
        level++;
        for (Object child : children) {
            ((Entity03)child).traverse(levels);
        }
        level--;
        if (printThisLevel(levels)) {
            indent.setLength(indent.length() - 3);
        }
    }
}


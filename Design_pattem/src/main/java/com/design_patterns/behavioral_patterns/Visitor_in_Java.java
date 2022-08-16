package com.design_patterns.behavioral_patterns;

import java.util.ArrayList;

/**
 * @Author: linK
 * @Date: 2022/8/11 10:36
 * @Description TODO VisitorComposite1 is a basic Composite implementation with one recursive traversal method. VisitorComposite2 is a non-Visitor implementation that models "parsing" the hierarchical Composite with the collect() recursive traversal method. VisitorComposite3 is a Visitor implementation.
 * <p>
 * Highlights. VisitorComposite2 changes interface Component into an abstract class. It requires protected static members. VisitorComposite3 is "open for extension, but closed for modification". The interface Component remains an interface. Now that "collect" is an object, many of them can be created and can operate simultaneously (the previous static attributes would have required significant extra effort to provide this functionality). Drawback: the public interface of Leaf and Composite had to be extended.
 */
public class Visitor_in_Java {
    public static void main(String[] args) {
        Composite[] containers = new Composite[3];
        for (int i = 0; i < containers.length; i++) {
            containers[i] = new Composite();
            for (int j = 1; j < 4; j++) {
                // Leaf 节点   正常遍历添加 123 456 789
                containers[i].add(new Leaf(i * containers.length + j));
            }
        }
        for (int i = 1; i < containers.length; i++) {
            // 追加 后续元素
            containers[0].add(containers[i]);
        }
        // 输出字母加数据   遍历回调
        containers[0].traverse();
        System.out.println();
    }
}

/**
 * 组件接口
 */
interface Component {
    void traverse();
}

/**
 * 叶子实现
 */
class Leaf implements Component {
    private int number;

    public Leaf(int value) {
        this.number = value;
    }

    public void traverse() {
        System.out.print(number + " ");
    }
}

/**
 * 合成实现
 */
class Composite implements Component {
    private static char next = 'a';
    private ArrayList children = new ArrayList();
    private char letter = next++;

    public void add(Component c) {
        children.add(c);
    }

    public void traverse() {
        System.out.print(letter + " ");
        for (Object aChildren : children) {
            // 回调叶子节点 实现
            ((Component) aChildren).traverse();
        }
    }
}


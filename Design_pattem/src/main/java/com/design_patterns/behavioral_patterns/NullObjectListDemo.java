package com.design_patterns.behavioral_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:59
 * @Description TODO 我们将创建一个 NullList 类来表示空列表，而不是使用 null 来表示空列表。
 */
public class NullObjectListDemo {
}

interface ListVisitor {
    Object whenNonNullList(NonNullList host, Object param);

    Object whenNullList(NullList host, Object param);
}

abstract class List {
    public abstract List getTail();

    public abstract Object accept(ListVisitor visitor, Object param);
}

class NonNullList extends List {
    private Object head;
    private List tail;

    // Creates a list from a head and tail. Acts as "cons"
    public NonNullList(Object head, List tail) {
        this.head = head;
        this.tail = tail;
    }

    // for convenience we could add a constructor taking only the head to make 1 element lists.
    public Object getHead() {
        return head;
    }

    public List getTail() {
        return tail;
    }

    public Object accept(ListVisitor visitor, Object param) {
        // 返回一个非空列表
        return visitor.whenNonNullList(this, param);
    }
}

class NullList extends List {
    private static final NullList instance = new NullList();

    private NullList() {
    }

    public static NullList singleton() {
        return instance;
    }

    public List getTail() {
        return this;
    }

    public Object accept(ListVisitor visitor, Object param) {
        //返回一个空列表 （什么都不做） 而不是直接返回null
        return visitor.whenNullList(this, param);
    }
}
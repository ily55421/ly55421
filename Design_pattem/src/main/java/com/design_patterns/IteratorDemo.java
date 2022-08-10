package com.design_patterns;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:28
 * @Description TODO
 */
public class IteratorDemo {
    public static void main(String[] args) {
        IntSet set = new IntSet();
        // 偶数添加
        for (int i = 2; i < 10; i += 2) {
            set.add(i);
        }
        for (int i = 1; i < 9; i++) {
            System.out.print(i + "-" + set.isMember(i) + "  ");
        }

        // 3. Clients ask the collection object to create many iterator objects
        IntSet.Iterator it1 = set.createIterator();
        IntSet.Iterator it2 = set.createIterator();

        // 4. Clients use the first(), isDone(), next(), currentItem() protocol
        System.out.print("\nIterator:    ");
        for (it1.first(), it2.first(); !it1.isDone(); it1.next(), it2.next()) {
            System.out.print(it1.currentItem() + " " + it2.currentItem() + "  ");
        }

        // Java uses a different collection traversal "idiom" called Enumeration
        System.out.print("\nEnumeration: ");
        for (Enumeration e = set.getHashtable().keys(); e.hasMoreElements(); ) {
            System.out.print(e.nextElement() + "  ");
        }
        System.out.println();
        //1-false  2-true  3-false  4-true  5-false  6-true  7-false  8-true
        //Iterator:    8 8  6 6  4 4  2 2
        //Enumeration: 8  6  4  2
    }
}

class IntSet {
    private Hashtable ht = new Hashtable();

    // 1. Design an internal "iterator" class for the "collection" class
    public static class Iterator {
        private IntSet set;
        private Enumeration e;
        private Integer current;

        public Iterator(IntSet in) {
            set = in;
        }

        public void first() {
            //返回此哈希表中键的枚举。
            e = set.ht.keys();
            next();
        }

        public boolean isDone() {
            return current == null;
        }

        public int currentItem() {
            return current;
        }

        public void next() {
            try {
                // 下一个枚举
                current = (Integer) e.nextElement();
            } catch (NoSuchElementException e) {
                current = null;
            }
        }
    }

    public void add(int in) {
        ht.put(in, "null");
    }

    public boolean isMember(int i) {
        return ht.containsKey(i);
    }

    public Hashtable getHashtable() {
        return ht;
    }

    // 2. Add a createIterator() member to the collection class
    public Iterator createIterator() {
        return new Iterator(this);
    }
}



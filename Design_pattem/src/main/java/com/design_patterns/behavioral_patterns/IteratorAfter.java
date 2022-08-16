package com.design_patterns.behavioral_patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:36
 * @Description TODO After
 * Take traversal-of-a-collection functionality out of the collection and promote it to "full object status". This simplifies the collection, allows many traversals to be active simultaneously, and decouples collection algorithms from collection data structures.
 */
public class IteratorAfter {
    public static void main(String[] args) {
        IntegerBox2 integerBox = new IntegerBox2();
        for (int i = 9; i > 0; --i) {
            integerBox.add(i);
        }
        // getData() has been removed.
        // Client has to use Iterator. 客户端必须使用迭代器。
        IntegerBox2.Iterator firstItr = integerBox.getIterator();
        IntegerBox2.Iterator secondItr = integerBox.getIterator();
        for (firstItr.first(); !firstItr.isDone(); firstItr.next()) {
            System.out.print(firstItr.currentValue() + "  ");
        }
        System.out.println();
        // Two simultaneous iterations
        for (firstItr.first(), secondItr.first(); !firstItr.isDone(); firstItr.next(), secondItr.next()) {
            System.out.print(firstItr.currentValue() + " " + secondItr.currentValue() + "  ");
        }
        //9  8  7  6  5  4  3  2  1
        //9 9  8 8  7 7  6 6  5 5  4 4  3 3  2 2  1 1
    }
}

class IntegerBox2 {
    private List<Integer> list = new ArrayList<>();

    public class Iterator {
        private IntegerBox2 box;
        private java.util.Iterator iterator;
        private int value;

        public Iterator(IntegerBox2 integerBox) {
            box = integerBox;
        }

        public void first() {
            iterator = box.list.iterator();
            next();
        }

        public void next() {
            try {
                value = (Integer) iterator.next();
            } catch (NoSuchElementException ex) {
                value = -1;
            }
        }

        public boolean isDone() {
            return value == -1;
        }

        public int currentValue() {
            return value;
        }
    }

    public void add(int in) {
        list.add(in);
    }

    public Iterator getIterator() {
        return new Iterator(this);
    }
}


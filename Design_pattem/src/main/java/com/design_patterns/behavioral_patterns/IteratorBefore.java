package com.design_patterns.behavioral_patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:32
 * @Description TODO Before
 * The class SomeClassWithData provides access to its internal data structure. Clients can accidentally or maliciously trash that data structure.
 */
public class IteratorBefore {
    public static void main(String[] args) {
        IntegerBox box = new IntegerBox();
        for (int i = 9; i > 0; --i) {
            box.add(i);
        }
        Collection integerList = box.getData();
        for (Object anIntegerList : integerList) {
            System.out.print(anIntegerList + "  ");
        }
        System.out.println();
        integerList.clear();
        integerList = box.getData();
        System.out.println("size of data is: " + integerList.size());
        //9  8  7  6  5  4  3  2  1
        //size of data is: 0
    }
}
class IntegerBox {
    private final List<Integer> list = new ArrayList<>();

    public void add(int in) {
        list.add(in);
    }

    public List getData() {
        return list;
    }
}


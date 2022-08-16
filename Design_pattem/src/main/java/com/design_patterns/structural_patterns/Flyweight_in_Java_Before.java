package com.design_patterns.structural_patterns;

import java.util.Arrays;

/**
 * @Author: linK
 * @Date: 2022/8/12 9:49
 * @Description TODO Flyweight design pattern
 * Discussion. Trying to use objects at very low levels of granularity is nice,
 * but the overhead may be prohibitive. Flyweight suggests removing the non-shareable state from the class, and having the client supply it when methods are called. This places more responsibility on the client, but, considerably fewer instances of the Flyweight class are now created. Sharing of these instances is facilitated by introducing a Factory class that maintains a "cache" of existing Flyweights.
 * 享元设计模式
 *
 * 讨论。尝试以非常低的粒度级别使用对象是不错的，但开销可能会令人望而却步。
 * Flyweight 建议从类中删除不可共享的状态，并在调用方法时让客户端提供它。
 * 这将更多的责任放在客户端上，但是现在创建的享元类的实例要少得多。
 * 通过引入维护现有享元的“缓存”的工厂类来促进这些实例的共享。
 */
public class Flyweight_in_Java_Before {
    public static final int ROWS = 6, COLS = 10;

    public static void main( String[] args ) {
        // 创建对象数组
        Gazillion[][] matrix = new Gazillion[ROWS][COLS];
        for (int i=0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                // 创建 ROWS * COLS 个对象
                matrix[i][j] = new Gazillion(COLS);
            }
        }
        for (int i=0; i < ROWS; i++) {
            for (int j=0; j < COLS; j++) {
                // 输出对象规格
                matrix[i][j].report();
            }
            System.out.println();
        }
        //测试创建对象个数
        matrix[ROWS-1][COLS-1].printNum();

        Arrays.asList(matrix).stream().forEach(System.out::println);

    }
}
class Gazillion {
    private static int num = 0;
    private int row, col;

    public Gazillion(int maxPerRow) {
        // 10  初始
        row = num / maxPerRow;
        col = num % maxPerRow;
        num++;
    }

    void report() {
        System.out.print(" " + row + col);
    }
    void printNum(){
        System.out.println(" "+num);
    }
}


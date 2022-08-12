package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 13:39
 * @Description TODO In this refactoring, the "row" state is considered shareable (within each row anyways), and the "col" state has been externalized (it is supplied by the client when report() is called).
 * 在此重构中，“行”状态被认为是可共享的（无论如何在每一行内），
 * 并且“col”状态已被外部化（它由客户端在调用 report() 时提供）。
 */
public class Flyweight_in_java_After {
    public static final int ROWS = 6, COLS = 10;

    public static void main(String[] args) {
        // 统一申请数组空间
        Factory02 theFactory02 = new Factory02(ROWS);
        for (int i=0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                // 工厂添加   getFlyweight(i) 确定当前行    通过调用report()方法输出 当前列
                theFactory02.getFlyweight(i).report(j);
            }
            //
            System.out.println();
        }
    }
}

class Gazillion02 {
    private int row;

    public Gazillion02(int row) {
        this.row = row;
        System.out.println("ctor: " + this.row);
    }

    void report(int col) {
        System.out.print(" " + row + col);
    }
}

class Factory02 {
    private Gazillion02[] pool;

    public Factory02(int maxRows) {
        pool = new Gazillion02[maxRows];
    }

    public Gazillion02 getFlyweight(int row) {
        if (pool[row] == null) {
            pool[row] = new Gazillion02(row);
        }
        return pool[row];
    }
}


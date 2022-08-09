package com.javaBase._02;

import java.util.Scanner;

/*14.键盘录入两个数，求两个数的和
键盘录入两个数，求两个数的最大值
键盘录入两个数据，比较这两个数据是否相等
键盘录入三个数据，获取这三个数据中的最大值*/

public class work_No_14_4 {
    private static Scanner s1;

    public static void main(String[] args) {
        s1 = new Scanner(System.in);
//		键盘录入三个数据，获取这三个数据中的最大值
        System.out.println("请输出三个整数，求最大值：回车结束");
        int a3 = s1.nextInt();
        int b3 = s1.nextInt();
        int c3 = s1.nextInt();
        int line = a3 > b3 ? a3 : b3;
        int max1 = line > c3 ? line : c3;
        System.out.println("三个数的最大值：" + max1);
        System.out.println("-------------");
    }
} 
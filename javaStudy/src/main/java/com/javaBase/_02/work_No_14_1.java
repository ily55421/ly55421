package com.javaBase._02;

import java.util.Scanner;

/*14.键盘录入两个数，求两个数的和
键盘录入两个数，求两个数的最大值
键盘录入两个数据，比较这两个数据是否相等
键盘录入三个数据，获取这三个数据中的最大值*/

// 录入两个数，求和
public  class work_No_14_1{
	private static Scanner s1;

	public static void main(String[] args) {
		s1 = new Scanner(System.in);
		System.out.println("请输出两个整数求和：回车结束");
		int a=s1.nextInt();
		int b=s1.nextInt();
		int sum=a+b;
		System.out.println("两个数之和："+sum);
		System.out.println("-------------");
	}
} 
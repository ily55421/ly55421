package com.javaBase._02;
import java.util.Scanner;

/*14.键盘录入两个数，求两个数的和
键盘录入两个数，求两个数的最大值
键盘录入两个数据，比较这两个数据是否相等
键盘录入三个数据，获取这三个数据中的最大值*/

public  class work_No_14_2{
	private static Scanner s1;

	public static void main(String[] args) {
		s1 = new Scanner(System.in);
//		录入两个数，求两个数的最大值
		System.out.println("请输出两个整数，比较最大值：回车结束");
		int a1=s1.nextInt();
		int b1=s1.nextInt();
		int max=a1>b1?a1:b1;
		System.out.println("两个数的最大值："+max);
		System.out.println("-------------");
	}
} 
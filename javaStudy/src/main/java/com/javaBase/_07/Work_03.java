package com.javaBase._07;

import java.util.Scanner;

//3.定义一个方法，该方法的功能是判断某个变量是奇数还是偶数
public class Work_03 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个数：");
		int i=s1.nextInt();
		getNum(i);




	}
	public static void getNum(int i) {
		if(i%2==0&&i>=0) {
			System.out.println(i+"是偶数");
		}else if(i%2==1&&i>0) {
			System.out.println("是奇数");
		}else {
			System.out.println("输入有误");
		}
	}
}

package com.javaBase._07;

import java.util.Scanner;

//2.定义一个方法，该方法的功能是打印几几乘法表
public class Work_02 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个数：");
		int a=s1.nextInt();
		getProduct(a);

	}
	public static void getProduct(int num) {
//		Scanner s1=new Scanner(System.in);
//		int num=s1.nextInt();
		System.out.println(num+" "+num+"乘法表");
		for(int i=1;i<=num;i++) {
			for(int j=1;j<=i;j++) {
				System.out.print(i+"*"+j+"="+i*j+"\t");
			}
			System.out.println();
		}
	}
}

package com.javaBase._07;

import java.util.Scanner;

//4.定义一个方法，该方法的功能是打印n到m之间的所有的奇数
public class Work_04 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入两个整数：后一个的值比前一个的值要大");
		int n=s1.nextInt();
		int m=s1.nextInt();
		getNum(n,m);
	}


	public static void getNum(int i,int j) {
		for(int num=i;num<=j;num++) {
			if(num%2==1&&num>0) {
				System.out.println(num);
			}else {
				System.out.println("输入错误");
				return;
			}

		}

	}
}

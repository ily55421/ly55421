package com.javaBase._03._switch;

import java.util.Scanner;

public class demo01 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入你的功法等级");
		int i=s1.nextInt();
		switch(i) {
			case 1:
				System.out.println("功法小乘"+i);
				break;
			case 2:
				System.out.println("功法筑基"+i);
				break;
			case 3:
				System.out.println("功法大乘"+i);
				break;
			default:
				System.out.println("你已跳出五行");
				break;

		}



	}
}

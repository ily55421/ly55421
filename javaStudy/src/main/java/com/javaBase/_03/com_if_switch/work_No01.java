package com.javaBase._03.com_if_switch;
/*1. 判断奇数偶数
需求：键盘录入一个数，判断该数字是奇数还是偶数*/


import java.util.Scanner;

//	判断奇数偶数
public class work_No01 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个数：(判断奇数偶数)");
		int sum=s1.nextInt();
		if(sum%2==0) {
			System.out.println(sum+"\t"+"是偶数");
		}else if(sum%2==1){
			System.out.println(sum+"\t"+"是奇数");
		}else {
			System.out.println("输入有误，请重试");
		}
		s1.close();
		System.out.println("判断结束");
	}
}


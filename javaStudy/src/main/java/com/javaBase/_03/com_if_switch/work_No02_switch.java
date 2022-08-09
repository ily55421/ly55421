package com.javaBase._03.com_if_switch;
/*2. 判断春夏秋冬(ifelse/swtich)
需求：键盘录入一个数(代表月份)，判断该数字是哪个季节
春天：3,4,5
夏天：6,7,8
秋天：9,10,11
冬天：12,1,2*/

import java.util.Scanner;
//	用switch语句判断春夏秋冬
public class work_No02_switch {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个月份:");
		int num=s1.nextInt();
		switch(num) {
			case 1:
			case 2:
				System.out.println("当前月份是冬天");
				break;
			case 3:
			case 4:
			case 5:
				System.out.println("当前月份是春天");
				break;
			case 6:
			case 7:
			case 8:
				System.out.println("当前月份是夏天");
				break;
			case 9:
			case 10:
			case 11:
				System.out.println("当前月份是秋天");
				break;
			case 12:
				System.out.println("当前月份是冬天");
				break;
			default:
				System.out.println("输入月份有误，请重试");

		}
		s1.close();
		System.out.println("判断结束");
	}
}

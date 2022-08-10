package com.javaBase._03.com_if_switch;
/*4. 判断星期数(switch)
	需求：键盘录入一个数字，判断该数字是星期几
	1-7
*/
import java.util.Scanner;

public class work_No04 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请在1-7之间输入一个数字:（判断是星期几）");
		int a=s1.nextInt();
		switch(a) {
			case 1:
				System.out.println("星期一");
				break;
			case 2:
				System.out.println("星期二");
				break;
			case 3:
				System.out.println("星期三");
				break;
			case 4:
				System.out.println("星期四");
				break;
			case 5:
				System.out.println("星期五");
				break;
			case 6:
				System.out.println("星期六");
				break;
			case 7:
				System.out.println("星期天");
				break;
			default:
				System.out.println("输入错误");

		}
		s1.close();
		System.out.println("判断结束");
	}
}

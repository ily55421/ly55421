package com.javaBase._03.com_if_switch;

/*2. 判断春夏秋冬(ifelse/swtich)
需求：键盘录入一个数(代表月份)，判断该数字是哪个季节
春天：3,4,5
夏天：6,7,8
秋天：9,10,11
冬天：12,1,2*/
import java.util.Scanner;
//	用if语句判断春夏秋冬
public class work_No02_if {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个月份数字:（判断是什么季节）");
		int num=s1.nextInt();
		if(num>2&&num<6){
			System.out.println("当前月份是春天");
		}else if(num>5&&num<9){
			System.out.println("当前月份是夏天");
		}else if(num>8&&num<12){
			System.out.println("当前月份是秋天");
		}else if(num==12||num<3&&num>0) {
			System.out.println("当前月份是冬天");
		}else {
			System.out.println("输入有误，请重试");
		}
		s1.close();
		System.out.println("判断结束");
	}
}

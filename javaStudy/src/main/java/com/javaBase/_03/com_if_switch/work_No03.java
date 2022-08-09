package com.javaBase._03.com_if_switch;
/*3. 考试奖励
需求：键盘录入一个分数，判断该分数是优秀，还是良，还是及格，还是不及格
优秀：100-90
良：89-70
及格：69-60
不及格：60-
*/
import java.util.Scanner;

public class work_No03 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个学生分数:");
		int a=s1.nextInt();
		if(a<=100&&a>=90) {
			System.out.println("成绩优秀");
		}else if(a<=89&&a>=70) {
			System.out.println("成绩良");
		}else if(a<=69&&a>=60) {
			System.out.println("成绩及格");
		}else if(a<60&&a>=0) {
			System.out.println("成绩不及格");
		}else {
			System.out.println("输入错误");
		}
		s1.close();
		System.out.println("判断结束");
	}
}
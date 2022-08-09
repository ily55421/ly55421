package com.javaBase._03._if;
import java.util.Scanner;

public class demo01 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入一个三位数");
		int num=s1.nextInt();

		int ge=num/1%10;
		int shi=num/10%10;
		int bai=num/100%10;
		System.out.println(ge);
		System.out.println(shi);
		System.out.println(bai);
	}
}

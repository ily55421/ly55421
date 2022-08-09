package com.javaBase._04;

import java.util.Scanner;


public class demo05 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入你要打印的直角三角形行数");
		int s=s1.nextInt();
		for(int i=0;i<=s;i++) {
			
			for(int j=0;j<=i;j++) {
				System.out.print("*");
			}
			System.out.println();
		}
	}
}

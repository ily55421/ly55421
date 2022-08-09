package com.javaBase._04;

public class demo03 {
	public static void main(String[] args) {
		for(int i=1;i<=4;i++) {
//			i为打印的列数
			for(int j=1;j<=5-i;j++) {
//				j为打印的行数
				System.out.print("*");
			}
			System.out.println();
		}
	}
}

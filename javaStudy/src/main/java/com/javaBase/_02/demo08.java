package com.javaBase._02;

public class demo08 {
	public static void main(String[] args) {
		int i=1;
		i=i++;//单独运算 i给自己赋值+1=2
		int j=i++;//先赋值，后运算j=2
		int k=i + ++i * i++;
//			k=2+3*3
		System.out.println(i);
		System.out.println(j);
		System.out.println(k);
	}
}

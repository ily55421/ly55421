package com.javaBase._02;


public class demo02 {
	public static void main(String[] args) {
		byte b1=1;
		byte b2=2;
//		byte b3=b1+b2;直接编译运算会报错
		byte b3=(byte)(b1+b2);//强制转化后可以进行计算
		System.out.println(b3);
		b1=127;
		byte b4=(byte)(b1+b2);//强制转化后可以进行计算
		System.out.println(b4);



		byte b=120+3;
		System.out.println(b);
	}
}
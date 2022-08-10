package com.javaBase._04;



public class demo06 {
	public static void main(String[] args) {
//		格式一： 数据类型 对象名 []=new 数据类型 [数组长度]
		int a [] =new int [5];
		a [0]= 3;
		a [1]= 4;
		a [2]= 5;
		a [3]= 6;
		a [4]= 7;
		int a1=a[0];
		int a2=a[1];
		int a3=a[2];
		int a4=a[3];
		int a5=a[4];
		int [] s =new int [6];
		int [] x =new int [] {1,2,3,4,5};		
		int [] a6= {1,3,5,12};
		System.out.println(a1+a2+a3+a4+a5);
		System.out.println(a[3]);
		System.out.println(a6[3]);
		
		
	}
}

package com.javaBase._07;
//1.定义一个方法，该方法的功能是打印两个数的和
public class Work_01 {
	public static void main(String[] args) {
		int a=getSum(3,4);	//赋值一个int类型变量a来接受getSum3+4的和值
		System.out.println(a);
		getSum01(3,5);

	}
	public static void  getSum01(int a,int b) {
		int sum=a+b;
		System.out.println(sum);
	}
	//	定义一个getSum方法，求和
	public static int getSum(int i,int j) {
		int sum=i+j;
		return sum;
	}
}

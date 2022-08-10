package com.javaBase._06.fuction;

public class Method {
	public static void main(String[] args) {
		System.out.println(Method.getDifference(3, 4));
		getSum(3,2);
	}

	//	创建方法求两个数的和
	public static int getSum(int a,int b) {

		int sum=a+b;
		return sum;
	}
	//	创建方法求两个数的差
	private static int getDifference(int c,int d) {
		int difference=c-d;
		return difference;
	}
	//	创建方法求两个数的乘积
	private static int getProduct(int e,int f) {
		int product=e*f;
		return product;
	}
	//	创建方法求两个数的除数
	private static int getDivisor(int e,int f) {
		int divisor=e/f;
		return divisor;
	}
	public static byte aaa() {
		return 1;
	}
	public static short bbb() {
		return 1;
	}
	public static int ccc() {
		return 1;
	}
	public static long ddd() {
		return 1L;
	}
	public static float eee() {
		return 1f;
	}
	public static double fff() {
		return 1;
	}
	public static char ggg() {
		return 'a';
	}
	public static String HHH() {
		return "123";
	}
	public static int [] iii(){
		int [] arr= {2,3,4};
		return arr;
	}

}

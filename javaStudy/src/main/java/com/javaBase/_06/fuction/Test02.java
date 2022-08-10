package com.javaBase._06.fuction;

public class Test02 {
	public static void main(String[] args) {
		String [] a=new String[3];
		System.out.println(Method02.jjj());

		System.out.println(Method02.jjj());
		Method02.jjj();//单独调用jjj
//		System.out.println(Method02.kkk());
		Method02.kkk();


		int b=Method.getSum(1,2);//赋值调用，将getSum的结果赋值给b
		System.out.println(b);
	}
}

package com.javaBase._02;

/*9.已知有三位数，将其拆分为个位，十位，百位，打印到控制台
123
1
2
3*/

public class work_No_09 {
	public static void main(String[] args) {
		int i=123;
		int b1=i/100;
//		取百位的除数
		int s1=(i-100*b1)/10;
//		去掉百位之后的余值，再取十位的除数
		int g1=i%10;
//		取个位的模
		System.out.println("百位："+b1+"\n"+"十位："+s1+"\n"+"个位:"+g1);
	}
}

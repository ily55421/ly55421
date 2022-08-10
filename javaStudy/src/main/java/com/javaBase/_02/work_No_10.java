package com.javaBase._02;

/*10.通过三元运算符来实现三个哥们比身高*/

public class work_No_10 {
	public static void main(String[] args) {
//
		int a=187,b=177,c=167;
		int line=a>b?a:b;
		int first=line>c?line:c;
		System.out.println("三个哥们中最高是："+first);
//		int second=b>c?b:c;
//		System.out.println(second);
//		int three=b<c?b:c;
//		System.out.println(three);
	}
}

package com.javaBase._03.com_if_switch;
/*10.在控制台输出所有的水仙花数
 * 水仙花数：指一个三位数的，各位数字立方和等于其本身
 */

public class work_No10 {
	public static void main(String[] args) {
		int a=100;
		System.out.println("水仙数如下：");
		while(a<=999) {
			int b=a/100%10;
			int s=a/10%10;
			int g=a%10;
			int a1=b*b*b;
			int a2=s*s*s;
			int a3=g*g*g;
			int c=a1+a2+a3;
			if(c==a) {
				System.out.println(a);
			}
			a++;
		}
	}
}

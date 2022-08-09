package com.javaBase._03.com_if_switch;
/*11.统计水仙花数共有多少个
 */

public class work_No11 {
	public static void main(String[] args) {
		int a=100;
		int j=0;

		while(a<=999) {
			int b=a/100%10;
			int s=a/10%10;
			int g=a%10;
			int a1=b*b*b;
			int a2=s*s*s;
			int a3=g*g*g;
			int c=a1+a2+a3;
			if(c==a) {
				j++;
			}
			a++;
		}
		System.out.println("水仙花数量："+j+"个");
	}
}

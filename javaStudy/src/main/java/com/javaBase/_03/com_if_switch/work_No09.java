package com.javaBase._03.com_if_switch;
/*9. 求出1-100之间的奇数和
 */

public class work_No09 {
	public static void main(String[] args) {
		int s1=0;
		for(int i=1;i<=100;i++) {
			if(i%2==1) {
				s1+=i;
			}
		}
		System.out.println("奇数之和："+s1);
	}
}

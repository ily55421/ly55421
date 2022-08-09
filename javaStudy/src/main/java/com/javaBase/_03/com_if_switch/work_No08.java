package com.javaBase._03.com_if_switch;
/*8. 求出1-100之间的偶数和
 */

public class work_No08 {
	public static void main(String[] args) {
		int s1=0;
		for(int i=1;i<=100;i++) {
			if(i%2==0) {
				s1+=i;
			}
		}
		System.out.println("偶数之和："+s1);
	}
}

package com.javaBase._03.com_if_switch;
/*16.从任意一个数字开始报数，当你要报的数字是包含7或者7的倍数时都要说过，
需求，使用程序在控制台打印出1-100之间的满足逢七必过规则的数据
*/

public class work_No16 {
	public static void main(String[] args) {
		System.out.println("1-100之间的满足逢七必过规则的数据:");
		int b=0;
		for(int a=1;a<=100;a++) {
			if(a%7==0||a%100/10==7||a%10==7) {

			}else {
				System.out.print(a+"\t");
				b++;
				if(b%5==0) {
					System.out.println();
				}
			}


		}

	}
}

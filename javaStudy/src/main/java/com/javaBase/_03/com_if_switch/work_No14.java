package com.javaBase._03.com_if_switch;
/*14.在控制台上输出九九乘法表
 */

public class work_No14 {
	public static void main(String[] args) {

		for(int i=1;i<=9;i++) {
			for(int j=1;j<=i;j++) {
				System.out.print(j+"*"+i+"="+i*j+"\t");
			}
			System.out.println();
		}
	}
}

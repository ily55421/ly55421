package com.javaBase._03.com_if_switch;
/*13.在控制台上输出一个由星星组成的正三角形图案
 */

public class work_No13 {
	public static void main(String[] args) {
		for(int i=1;i<=3;i++) {
			for(int j=1;j<=3-i;j++) {
				System.out.print(" ");
			}
			for(int c=1;c<=i;c++) {
				System.out.print("* ");
			}
			System.out.println();
		}

	}
}

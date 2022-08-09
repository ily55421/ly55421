package com.javaBase._03.com_if_switch;
/*18.分析以下需求，并用代码实现：
	1.打印1到100之内的整数，但数字中包含9的要跳过
	2.每行输出5个满足条件的数，之间用空格分隔
	3.如：1 2 3 4 5

*/


//程序如下
public class work_No18 {
	public static void main(String[] args) {
		System.out.println("1到100之内的整数中，不包含9如下：");
		int a=0;
		for(int i=1;i<=100;i++) {
			if(i%10!=9&&i/10!=9) {
				System.out.print(i+"\t");
				a++;
				if(a%5==0) {
					System.out.println();
				}
			}
		}
	}
}

package com.javaBase._07;

/*9.分析以下需求，并用代码实现
1.键盘录入长方形的长和宽
	定义方法计算该长方形的周长,并在main方法中打印周长
2.键盘录入长方形的长和宽
	定义方法计算该长方形的面积,并在main方法中打印面积
3.键盘录入圆的半径
	定义方法计算该圆的周长,并在main方法中打印周长
4.键盘录入圆的半径
	定义方法计算该圆的面积,并在main方法中打印面积*/

import java.util.Scanner;

//1.键盘录入长方形的长和宽
//定义方法计算该长方形的周长,并在main方法中打印周长
public class Work_09_01 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请输入长方形的长和宽");
		int a=s1.nextInt();
		int b=s1.nextInt();
		getNum(a,b);
	}

	//	定义长方形求周长的方法
	public static void getNum(int i,int j) {
		int Zhou=2*(i+j);
		System.out.println("长方形的周长："+Zhou);
	}

}

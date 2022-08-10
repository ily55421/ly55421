package com.javaBase._02;

/*11.找出以下代码的错误，并修改
public class AAA {
	public static void main(String[] args) {
		int a;
		System.out.println(a);
		{
			int c = 20;
			System.out.println(c);
		}
		c = 30;
		System.out.println(c);
	}
}

public class BBB {
	public static void main(String[] args) {
		byte b = 3;
		b = b + 4;
		System.out.println("b=" + b);
	}
}

public class CCC {
	public static void main(String[] args) {
		int x = 2;
		{
			int y = 6;
			System.out.println("x is " + x);
			System.out.println("y is " + y);
		}
		y = x;
		System.out.println("x is " + x);
	}
}*/

//	程序修改如下:

class work_No_11_CCC {
	public static void main(String[] args) {
		int x = 2;
		int y = 6;
		System.out.println("x is " + x);
		System.out.println("y is " + y);
		y = x;
		System.out.println("x is " + x);
	}
}





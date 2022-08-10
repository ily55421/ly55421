package com.javaBase._07;



//5.定义一个方法，该方法的功能是遍历数组
public class Work_05 {
	public static void main(String[] args) {
		int [] b= {2,3,5};
		getNum01(b);
		getNum02(b);
	}


	public static void getNum01(int []a) {//遍历方式1
		for(int i=0;i<a.length;i++) {
			System.out.println(a[i]);
		}
	}

	public static void getNum02(int []a) {//遍历方式2
		for(int i: a) {
			System.out.println(i);
		}
	}
}

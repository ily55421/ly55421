package com.javaBase._07;

//	6.定义一个方法，该方法的功能是获取数组中元素的最大值
public class Work_06 {
	public static void main(String[] args) {
		int [] arr= {3,4,5,6,7};
		getNum(arr);

	}

	//	定义获取数组中最大值的方法
	public static void getNum(int[] a) {
		int temp=a[0];
		for(int i=1;i<a.length;i++) {
			if(temp<a[i]) {
				temp=a[i];
			}
		}
		System.out.println("最大值："+temp);
	}
}

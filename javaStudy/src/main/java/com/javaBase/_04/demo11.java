package com.javaBase._04;


//	倒序输出
public class demo11 {
	public static void main(String[] args) {
	
		int [] a= {1,3,5,7,9};//定义一个数组
		System.out.println("顺序改变前：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.println(a[i]);
		}
		for(int temp=0;temp<a.length/2;temp++) {//定义一个交换循环
			int a1=0;//定义中间变量a1
			a1=a [temp];
			a[temp]=a[a.length-1-temp];//进行数组首尾互换
			a[a.length-1-temp]=a1;
		}
		System.out.println("顺序改变后：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.println(a[i]);
		}
		
		
	}
}

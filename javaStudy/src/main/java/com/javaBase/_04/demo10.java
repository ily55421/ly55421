package com.javaBase._04;

import java.util.Scanner;


public class demo10 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in); 
		System.out.println("请输入一个数：");
		int a1=s1.nextInt();
		int [] a= {1,3,6,2,5,4};//定义一个数组
		for(int a2=0;a2<a.length;a2++) {
			if(a1==a[a2]) {//当输入的值与数组遍历的值相等时输出下标
				System.out.println("该数的索引是："+a2);
				break;
			}else if(a2==a.length-1) {//遍历到最后一位，没有相等的数返回-1
				System.out.println(-1);
			}
		}
		
		
		
	}
}

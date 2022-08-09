package com.javaBase._04;

//	获取数组最值

public class demo09 {
	public static void main(String[] args) {
		int [] a= {1,3,6,2,5,4};//定义一个数组
		int b=a[0];//定义一个变量b，用以接受最大值
//		for(int c=0;c<a.length;c++) {//定义变量c，遍历 
//			if(a[c]>b) {
//				b=a[c];//当数组值大于变量b时，赋值给b继续参与循环
//			}	
//		}
//		System.out.println(b);//输出最大值变量b
		
		
		
		for(int c2=1;c2<a.length;c2++) {//定义变量c，遍历 
			if(a[c2]<b) {
				b=a[c2];//当数组值大于变量b时，赋值给b继续参与循环
			}	
		}
		System.out.println(b);//输出最大值变量b
		/*int c1=0;
		while (c1<a.length) {
			if(a[c1]>b) {
				
			}
		}*/
		
		
	}
}

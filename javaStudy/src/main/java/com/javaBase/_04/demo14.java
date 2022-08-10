package com.javaBase._04;


//	数组排序	真冒泡算法
public class demo14 {
	public static void main(String[] args) {
		int [] a= {1,6,2,5,4,3,2};//定义一个数组
		System.out.println("排序前：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.print(a[i]+" ");
		}
		int count1=0;//定义一个中间变量
//		从后往前比较选择比较
	/*	for(int j=1;j<a.length;j++) {//定义一个遍历
			for(int a1=a.length-1;a1>=1;a1--) {//定义一个循环比较
				if(a[a1-1]>a[a1]) {//第一个和后一个比较
					count1=a[a1-1];
					a[a1-1]=a[a1];
					a[a1]=count1;
				}
				if(){
				}
			}
		}*/
//		从前往后依次选择比较
	/*	for(int j=1;j<a.length;j++) {//定义一个遍历
			for(int i=j+1;i<a.length;i++) {//定义一个循环比较
				
				if(a[j]>a[i]) {//第一个和后续每一个比较
					count1=a[j];
					a[j]=a[i];
					a[i]=count1;
				}
				
					
			}
		}*/
		System.out.println();
		System.out.println("排序后：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.print(a[i]+" ");
		}
	}
}

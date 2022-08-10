package com.javaBase._04;


//	数组排序 假冒泡算法
public class demo13 {
	public static void main(String[] args) {
		int [] a= {1,6,2,5,4,3,2};//定义一个数组
		System.out.println("排序前：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.print(a[i]+" ");
		}
		int count1=0;//定义一个中间变量
		/*for(int temp=0;temp<=a.length;temp++) {//定义一个遍历
			for(int a1=1;a1<a.length-temp;a1++) {//定义一个循环比较
				if(a[temp]<a[temp+a1]) {//第一个和后一个比较
					a[temp]=a[temp];//小于则位置不变
				}else {//否则与后项交换位置
					count1=a[temp];
					a[temp]=a[a1+temp];
					a[a1+temp]=count1;
				}
			}
		}*/
	/*	for(int ) {
			
		}*/
		System.out.println();
		System.out.println("排序后：");
		for(int i=0;i<a.length;i++) {//遍历输出
			System.out.print(a[i]+" ");
		}
	}
}

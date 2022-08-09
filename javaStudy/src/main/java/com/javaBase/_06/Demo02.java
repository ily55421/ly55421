package com.javaBase._06;

import java.util.Arrays;
//		二维数组格式练习
public class Demo02 {
	public static void main(String[] args) {
//		二维数组格式一：
//		数据类型 【】【】数组名=new 数据类型 【】【】；
		int [][] arr=new int [2] [];
		int [][] arr1=new int [2][3];
		int [][] arr2={{2,3,4},{1,2},{7,2}};
		System.out.println(Arrays.toString(arr2[0]));
		System.out.println(Arrays.toString(arr2[1]));
		System.out.println(Arrays.toString(arr2[2]));
		System.out.println(arr1[1][2]);
		for(int i=0;i<arr2.length;i++) {
			for(int j=0;j<arr2[i].length;j++) {
				System.out.print(arr2[i][j]+" ");
			}
			System.out.println();
		}
		int [] arr_1={3,2};
		int [] arr_2={5,6};
		arr[0]=arr_1;
		arr[0]=arr_2;
		System.out.println(arr);
		System.out.println(arr[0]);
		System.out.println(arr[1]);

	}
}


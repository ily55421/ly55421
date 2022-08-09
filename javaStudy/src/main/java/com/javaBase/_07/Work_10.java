package com.javaBase._07;

import java.util.Scanner;

/*10.分析以下需求，并用代码实现
1.键盘录入班级人数
2.根据录入的班级人数创建数组
3.通过键盘录入将班级每个人的分数录入到数组容器中
4.要求:
	(1)打印该班级的不及格人数
	(2)打印该班级的平均分
	(3)演示格式如下:
		请输入班级人数:
		键盘录入:100
		请输入第一个学员分数
		键盘录入：20
		请输入第二个学员的分数
		键盘录入：98
		控制台输出:
			不及格人数:19
			班级平均分:87*/
public class Work_10 {
	public static void main(String[] args) {
		Scanner s1=new Scanner(System.in);
		System.out.println("请录入班级的人数：");
		int num=s1.nextInt();
		int [] classArr= new int [num];//	通过键盘录入的人数为数组的长度
		getArrays(classArr);
		getNum(classArr);

	}
	//	记录循环录入成绩的方法
	public static void getArrays(int[] b) {
		Scanner s2=new Scanner(System.in);
		System.out.println("请录入学生的成绩：");
		for(int i=0;i<b.length;i++) {
			int num=s2.nextInt();
			b[i]=num;
		}
	}
	//	计算平均分和不及格人数的方法
	public static void getNum(int[] a) {
		int count=0;
		double sum=0;
		for(int i=0;i<a.length;i++) {
			if(a[i]<60) {
				count++;
			}
		}
		for(int j=0;j<a.length;j++) {
			sum+=a[j];
		}
		double Avg=sum/a.length;
		System.out.println("不及格人数："+count);
		System.out.println("平均分："+Avg);
	}
}

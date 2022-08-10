package com.javaBase._07;

/*8.分析以下需求，并用代码实现(每个小需求都需要封装成方法)
1.判断两个数据是否相等(整数和小数)
2.获取两个数中较大的值(整数和小数)
3.获取两个数中较小的值(整数和小数)
4.否能用一个方法实现3和4的两个功能*/

public class Work_08_03 {
	public static void main(String[] args) {
		getNum01(2.3,3);
	}

	//	3.获取两个数中较小的值(整数和小数)
	public static void getNum01(double i,double j) {
		if(i<j) {
			double Min=i;
			System.out.println("最小值："+Min);
		}else {
			double Min=j;
			System.out.println("最小值："+Min);
		}
	}
}

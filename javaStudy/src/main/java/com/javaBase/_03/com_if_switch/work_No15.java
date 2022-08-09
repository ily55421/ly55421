package com.javaBase._03.com_if_switch;
/*15.世界最高峰是珠穆朗玛峰(8844.43米=8844430毫米),假如有一张足够大的纸，它的厚度是0.1毫米，
请问，需要折叠多少次，可以折成珠穆朗玛峰的高度
*/

public class work_No15 {
	public static void main(String[] args) {
		int a=0;
		for(double d=0.1;d<=8844430;d*=2) {
			a++;
		}
		System.out.println("需要折叠的次数："+a+"次");
	}
}

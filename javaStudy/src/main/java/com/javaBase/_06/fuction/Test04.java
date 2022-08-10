package com.javaBase._06.fuction;
//	测试输出顺序
public class Test04 {
	public static void main(String[] args) {
		aaa();
	}
	public static void aaa() {
		System.out.println(564);
		bbb();
	}
	public static void bbb() {
		ccc();
		System.out.println(454);
	}
	public static void ccc() {
		System.out.println(625);
	}

}

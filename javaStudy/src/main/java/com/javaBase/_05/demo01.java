package com.javaBase._05;

public class demo01 {
	public static void main(String[] args) {
		int a=0;
		do {
			a++;
			System.out.print(a);
		}
		while(a<3);
		System.out.println();
		while(a<10) {
			a++;
			System.out.print(a);
			a++;
		}
		System.out.println();
		for(int b=0;b<=10;b++) {
			System.out.print(b);
		}
	}
}

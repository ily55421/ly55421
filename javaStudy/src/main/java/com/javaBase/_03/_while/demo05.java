package com.javaBase._03._while;

public class demo05 {
	public static void main(String[] args) {
		int i=1,t=0;
		while(i<=10) {
			
			if (i%2==1) {
				t++;
			}
			i++;
		}
		System.out.println(t);
	}
}

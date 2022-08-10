package com.javaBase._02;

public class demo10 {
	public static void main(String[] args) {
		int i=2,j=3,t=0;
		System.out.println(i);
		System.out.println(j);
		/*t=i;
		i=j;
		j=t;*/
		t=i^j;
		i=i^t;
		j=t^j;
		System.out.println(i);
		System.out.println(j);
		
	
	}
}

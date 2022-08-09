package com.javaBase._03.com_if_switch;
/*19.看程序,完成以下问题
	public class Test {
		public static void main(String[] args) {
			for(int x=1; x<=10; x++) {
				if(x%3==0) {
					//()在此处填写代码
				}
				System.out.println("Java");
			}
		}
	}
	问题:
		1.想在控制台输出2次:"Java"   括号()里面应该填写什么?
		2.想在控制台输出7次:"Java"   括号()里面应该填写什么?
		3.想在控制台输出13次:"Java"   括号()里面应该填写什么?

*/
//3.想在控制台输出13次:"Java"   括号()里面应该填写什么?
public class work_No19_03 {
	public static void main(String[] args) {
		for(int x=1; x<=10; x++) {
			if(x%3==0) {
				System.out.println("Java");
			}

			System.out.println("Java");
		}
	}
}

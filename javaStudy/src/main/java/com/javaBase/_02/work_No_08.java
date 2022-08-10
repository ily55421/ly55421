package com.javaBase._02;
/*8.实现以下功能(办公用品报价单)

办公用品编号     办公用品名称         	 办公用品单价     计价单位     品质
    1        垃圾袋                       8.0         把          C
    2        听课证                       10.0        个          A
    3        小米彩虹电池             9.9         排         A

(1)用字符串常量实现上面的功能
(2)用不同的数据类型常量实现上面的功能
(3)用变量实现上面的功能
*/
public class work_No_08 {
	public static void main(String[] args) {
		System.out.println("(1)用字符串常量实现上面的功能:");
		System.out.println("办公用品编号	办公用品名称	办公用品单价	计价单位	品质");
		System.out.println("1	垃圾袋	8.0	把	C");
		System.out.println("2 	听课证	10.0 	个 	A");
		System.out.println("3	小米彩虹电池	9.9 	排	A");
		System.out.println("---------------------");

		System.out.println("(2)用不同的数据类型常量实现上面的功能:");
		System.out.println("办公用品编号"+"\t"+"办公用品名称"+"\t"+"办公用品单价"+"\t"+"计价单位"+"\t"+"品质");
		System.out.println(1+"\t"+"垃圾袋"+"\t"+8.0+"\t"+'把'+"\t"+'C');
		System.out.println(2+"\t"+"听课证"+"\t"+10.0+"\t"+'个'+"\t"+'A');
		System.out.println(3+"\t"+"小米彩虹电池"+"\t"+9.9+"\t"+'排'+"\t"+'A');
		System.out.println("---------------------");

		System.out.println("(3)用变量实现上面的功能:");
		String s1="办公用品编号	办公用品名称	办公用品单价	计价单位	品质";
		String s2="1	垃圾袋	8.0	把	C";
		String s3="2 	听课证	10.0 	个 	A";
		String s4="3	小米彩虹电池	9.9 	排	A";
		System.out.println(s1+"\n"+s2+"\n"+s3+"\n"+s4);


	}
}

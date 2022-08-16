package com.example.simplifying_conditional_expressions;

import java.util.Date;

/**
 * @Author: linK
 * @Date: 2022/8/15 15:19
 * @Description TODO 分解表达式
 */
public class Decompose_Conditional {
    /*
    问题
您有一个复杂的条件（if-then/else 或 switch）。

解决方案
将条件的复杂部分分解为单独的方法：条件、然后和其他。

为什么要重构
一段代码越长，就越难理解。当代码充满条件时，事情变得更加难以理解：
当您忙于弄清楚 then 块中的代码做了什么时，您忘记了相关条件是什么。
当您忙于解析 else 时，您忘记了 then 中的代码是做什么的。

好处
通过将条件代码提取到明确命名的方法中，您可以让以后维护代码的人（比如两个月后的您！）的生活更轻松。
这种重构技术也适用于条件中的短表达式。字符串 isSalaryDay() 比用于比较日期的代码更漂亮且更具描述性。

如何重构
通过提取方法将条件提取到单独的方法。
对 then 和 else 块重复该过程。

     */
}
class Decompose_Conditional_Before{
    private double quantity;
    private double winterRate;
    private double winterServiceCharge;
    private double summerRate;
    private double charge;
    private Date SUMMER_START;
    private Date SUMMER_END;

  double getPriceByDate(Date date){
      if (date.before(SUMMER_START) || date.after(SUMMER_END)) {
          charge = quantity * winterRate + winterServiceCharge;
      }
      else {
          charge = quantity * summerRate;
      }
      return charge;
  }
}

/**
 * 简化条件表达式
 */
class Decompose_Conditional_After{
    private double quantity;
    private double winterRate;
    private double winterServiceCharge;
    private double summerRate;
    private double charge;
    private Date SUMMER_START;
    private Date SUMMER_END;

    double getPriceByDate(Date date){
        if (isSummer(date)) {
            charge = summerCharge(quantity);
        }
        else {
            charge = winterCharge(quantity);
        }
        return charge;
    }

    private double winterCharge(double quantity) {
        return  quantity * winterRate + winterServiceCharge;
    }

    private double summerCharge(double quantity) {
        return charge = quantity * summerRate;
    }

    private boolean isSummer(Date date) {
        return date.before(SUMMER_START) || date.after(SUMMER_END);
    }
}

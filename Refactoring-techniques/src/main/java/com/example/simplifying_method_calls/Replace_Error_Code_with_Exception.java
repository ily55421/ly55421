package com.example.simplifying_method_calls;

/**
 * @Author: linK
 * @Date: 2022/8/15 17:48
 * @Description TODO 用异常替换错误码
 */
public class Replace_Error_Code_with_Exception {
/*
问题
方法返回一个表示错误的特殊值？

解决方案
改为抛出异常。

为什么要重构
返回错误代码是过程编程的过时保留。在现代编程中，错误处理由称为异常的特殊类执行。如果出现问题，您会“抛出”一个错误，然后该错误会被其中一个异常处理程序“捕获”。
在正常情况下被忽略的特殊错误处理代码被激活以响应。

好处
将代码从大量用于检查各种错误代码的条件中解放出来。异常处理程序是区分正常执行路径和异常执行路径的更简洁的方法。
异常类可以实现自己的方法，因此包含部分错误处理功能（例如用于发送错误消息）。
与异常不同，错误代码不能在构造函数中使用，因为构造函数必须只返回一个新对象。

缺点
异常处理程序可以变成类似 goto 的拐杖。避免这种情况！不要使用异常来管理代码执行。仅应抛出异常以通知错误或紧急情况。

如何重构
尝试一次只针对一个错误代码执行这些重构步骤。这将使您更容易将所有重要信息保留在您的脑海中并避免错误。
查找对返回错误代码的方法的所有调用，而不是检查错误代码，而是将其包装在 try/catch 块中。
在方法内部，不是返回错误代码，而是抛出异常。
更改方法签名，使其包含有关所引发异常的信息（@throws 部分）。


 */
}

class Replace_Error_Code_with_Exception_Before {
    private int balance;
    private int _balance;


    int withdraw(int amount) {
        if (amount > _balance) {
            return -1;
        } else {
            balance -= amount;
            return 0;
        }
    }
}

class Replace_Error_Code_with_Exception_After {
    private int balance;
    private int _balance;

    /**
     * 用异常替换错误码
     *
     * @param amount
     * @throws BalanceException
     */
    void withdraw(int amount) throws BalanceException {
        if (amount > _balance) {
            throw new BalanceException();
        }
        balance -= amount;
    }
}

class BalanceException extends Exception {
    //...
}
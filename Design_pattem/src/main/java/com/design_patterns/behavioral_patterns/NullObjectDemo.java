package com.design_patterns;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @Author: linK
 * @Date: 2022/8/10 14:56
 * @Description TODO
 */
public class NullObjectDemo {
    public static void main(String[] args) {
        Application app = new Application(new NullPrintStream());
        app.doSomething();
        // sum = 45 通过一个空对象  但什么都不做
        // 暂时留空

    }
}
class NullOutputStream extends OutputStream {
    public void write(int b) {
        // Do nothing
    }
}

class NullPrintStream extends PrintStream {
    public NullPrintStream() {
        super(new NullOutputStream());
    }
}

class Application {
    private PrintStream debugOut;
    public Application(PrintStream debugOut) {
        this.debugOut = debugOut;
    }

    public void doSomething() {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += i;
            // 调用空对象
            debugOut.println("i = " + i);
        }
        System.out.println("sum = " + sum);
    }
}



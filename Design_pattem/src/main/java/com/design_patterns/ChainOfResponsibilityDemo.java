package com.design_patterns;

import java.util.Random;

/**
 * @Author: linK
 * @Date: 2022/8/10 10:52
 * @Description TODO
 */
public class ChainOfResponsibilityDemo {
    public static void main( String[] args ) {
        Image[] inputImages = {new IR(), new IR(), new LS(), new IR(), new LS(), new LS()};
        Processor[] processors = {new Processor(), new Processor(), new Processor()};
        for (int i=0, j; i < inputImages.length; i++) {
            System.out.println("Operation #" + (i + 1) + ":");
            j = 0;
            // processors[j] 执行方法
            // inputImages[i] 处理链
            while (!processors[j].execute(inputImages[i])) {
                // 随机执行
                j = (j + 1) % processors.length;
            }
            System.out.println();
            //Operation #1:
            //Processor 1 - IR
            //
            //Operation #2:
            //   Processor 1 is busy
            //   Processor 2 is busy
            //Processor 3 - IR
            //
            //Operation #3:
            //   Processor 1 is busy
            //Processor 2 - LS
            //
            //Operation #4:
            //   Processor 1 is busy
            //Processor 2 - IR
            //
            //Operation #5:
            //Processor 1 - LS
            //
            //Operation #6:
            //   Processor 1 is busy
            //Processor 2 - LS
            //
        }
    }
}
interface Image {
    String process();
}

class IR implements Image {
    public String process() {
        return "IR";
    }
}

class LS implements Image {
    public String process() {
        return "LS";
    }
}

class Processor {
    private static final Random RANDOM = new Random();
    private static int nextID = 1;
    private int id = nextID++;

    public boolean execute(Image img) {
        if (RANDOM.nextInt(2) != 0) {
            System.out.println("   Processor " + id + " is busy");
            return false;
        }
        System.out.println("Processor " + id + " - " + img.process());
        return true;
    }
}

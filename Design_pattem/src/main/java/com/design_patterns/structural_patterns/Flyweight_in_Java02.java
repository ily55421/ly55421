package com.design_patterns.structural_patterns;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @Author: linK
 * @Date: 2022/8/12 13:59
 * @Description TODO
 * heavyweight ColorBoxes  ==>  ColorBox Flyweights and a Factory
 * (1 thread per ColorBox)         of pooled HandlerThreads
 */
public class Flyweight_in_Java02 {
    public static void main(String[] args) {
        int size = 8;
        int pause = 100;
        if (args.length > 0) {
            size = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            pause = Integer.parseInt(args[1]);
        }
        // 创建面板
        Frame frame = new Frame("ColorBoxes - 1 thread per ColorBox");
        // 设置创建具有指定行数和列数的网格布局。   布局
        frame.setLayout(new GridLayout(size, size));
        for (int i = 0; i < size * size; i++) {
            // 增加面板的盒子
            frame.add(new ColorBox(pause));
        }
        // 设置尺寸
        frame.setSize(500, 400);
        // 设置可见
        frame.setVisible(true);
        // 添加窗口监听  窗口关闭 系统停止
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        // 入参
        // java ColorBoxes 18 50
        //        produces 324 boxes/threads and 50 millisecond sleep()

    }
}

/**
 * Canvas组件表示屏幕上的一个空白矩形区域，应用程序可以在该区域上进行绘制，或者应用程序可以从中捕获来自用户的输入事件。
 */
class ColorBox extends Canvas implements Runnable {
    private int pause;
    private Color curColor = getColor();
    /**
     * 颜色数组
     */
    private static Color[] colors = {Color.black, Color.blue, Color.cyan,
            Color.darkGray, Color.gray, Color.green, Color.lightGray, Color.red,
            Color.magenta, Color.orange, Color.pink, Color.white, Color.yellow};

    public ColorBox(int p) {
        pause = p;
        new Thread(this).start();
    }

    private static Color getColor() {
        return colors[(int) (Math.random() * 1000) % colors.length];
    }

    public void run() {
        while (true) {
            curColor = getColor();
            repaint();
            try {
                Thread.sleep(pause);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void paint(Graphics g) {
        g.setColor(curColor);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}


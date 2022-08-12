package com.design_patterns;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * @Author: linK
 * @Date: 2022/8/12 13:47
 * @Description TODO Flyweight design pattern
 * Identify shareable state (intrinsic) and non-shareable state (extrinsic)
 * Create a Factory that can return an existing object or a new object
 * The client must use the Factory instead of "new" to request objects
 * The client (or a third party) must provide/compute the extrinsic state
 * 享元设计模式
 *
 * 识别可共享状态（内部）和不可共享状态（外部）
 *
 * 创建可以返回现有对象或新对象的工厂
 *
 * 客户端必须使用 Factory 而不是“new”来请求对象(再factory中 进行new)
 *
 * 客户端（或第三方）必须提供/计算外部状态
 */
public class Flyweight_in_Java {
    public static void main( String[] args ) {
        Random rn = new Random();
        Frame frame = new Frame("Flyweight Demo");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setLayout(new GridLayout(10, 10));
        // 1. Identify shareable and non-shareable state
        //    shareable - java.awt.Button label, non-shareable - java.awt.Button location
        for (int i=0; i < 10; i++) {
            for (int j=0; j < 10; j++)
                // 3. The client must use the Factory to request objects
            {
                frame.add(FlyweightFactory02.make(
                        Integer.toString(rn.nextInt(15))));
            }
        }
        frame.pack();
        frame.setVisible( true );
        FlyweightFactory02.report();
        //new Buttons - 15, "shared" Buttons - 85, 0 1 10 11 12 13 14 2 3 4 5 6 7 8 9
    }

}
class FlyweightFactory02 {
    private static Map treeMap = new TreeMap();
    private static int sharedButtons = 0;
    private static ButtonListener listener = new ButtonListener();

    public static java.awt.Button make(String num) {
        java.awt.Button button;
        if (treeMap.containsKey(num)) {
            // 1. Identify intrinsic state (java.awt.Button label)
            // 2. Return an existing object   [The same java.awt.Button cannot be added
            //    multiple times to a container, and, Buttons cannot be cloned.
            //    So - this is only simulating the sharing that the Flyweight
            //    pattern provides.]    共享对象
            // 返回一个现有对象 [同一个 java.awt.Button 不能多次添加到容器中，并且 Buttons 不能被克隆。所以 - 这只是模拟享元的共享
            button = new java.awt.Button(((java.awt.Button) treeMap.get(num)).getLabel());
            sharedButtons++;
        } else {
            // 2. Return a new object
            button = new java.awt.Button(num);
            treeMap.put(num, button);
        }
        button.addActionListener(listener);
        return button;
    }

    public static void report() {
        System.out.print("new Buttons - " + treeMap.size()
                + ", \"shared\" Buttons - " + sharedButtons + ", ");
        for (Object o : treeMap.keySet()) {
            System.out.print(o + " ");
        }
        System.out.println();
    }  }

class ButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        java.awt.Button button  = (java.awt.Button)e.getSource();
        java.awt.Component[] buttons = button.getParent().getComponents();
        int i = 0;
        for ( ; i < buttons.length; i++) {
            if (button == buttons[i]) {
                break;
            }
        }
        // 4. A third party must compute the extrinsic state (x and y)
        //    (the java.awt.Buttonlabel is intrinsic state)
        System.out.println("label-" + e.getActionCommand()
                + "  x-" + i/10   + "  y-" + i%10);
    }
}


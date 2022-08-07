package com.design.flyweightPattern;

/**
 * 使用该工厂，通过传递颜色信息来获取实体类的对象
 */
public class FlyweightPatternDemo {
    private static final String colors[] =
            {"Red", "Green", "Blue", "White", "Black"};

    public static void main(String[] args) {

        for (int i = 0; i < 20; ++i) {
            // 模拟随机颜色 生成对象
            Circle circle =
                    (Circle) ShapeFactory.getCircle(getRandomColor());
            circle.setX(getRandomX());
            circle.setY(getRandomY());
            circle.setRadius(100);
            circle.draw();
        }
        //Creating circle of color : White
        //Circle: Draw() [Color : White, x : 45, y :14, radius :100
        //Creating circle of color : Blue
        //Circle: Draw() [Color : Blue, x : 20, y :68, radius :100
        //Creating circle of color : Red
        //Circle: Draw() [Color : Red, x : 67, y :59, radius :100
        //Circle: Draw() [Color : Blue, x : 7, y :9, radius :100
        //Creating circle of color : Black
        //Circle: Draw() [Color : Black, x : 37, y :32, radius :100
        //Circle: Draw() [Color : Black, x : 5, y :58, radius :100
        //Circle: Draw() [Color : Red, x : 86, y :7, radius :100
        //Circle: Draw() [Color : Blue, x : 34, y :29, radius :100
        //Circle: Draw() [Color : Black, x : 73, y :96, radius :100
        //Circle: Draw() [Color : Black, x : 27, y :1, radius :100
        //Circle: Draw() [Color : Red, x : 95, y :55, radius :100
        //Circle: Draw() [Color : Black, x : 45, y :29, radius :100
        //Circle: Draw() [Color : Blue, x : 89, y :99, radius :100
        //Circle: Draw() [Color : Red, x : 97, y :70, radius :100
        //Circle: Draw() [Color : Blue, x : 46, y :90, radius :100
        //Circle: Draw() [Color : Black, x : 94, y :64, radius :100
        //Circle: Draw() [Color : Red, x : 88, y :46, radius :100
        //Circle: Draw() [Color : Red, x : 20, y :48, radius :100
        //Circle: Draw() [Color : Red, x : 87, y :13, radius :100
        //Creating circle of color : Green
        //Circle: Draw() [Color : Green, x : 5, y :55, radius :100
    }

    private static String getRandomColor() {
        return colors[(int) (Math.random() * colors.length)];
    }

    private static int getRandomX() {
        return (int) (Math.random() * 100);
    }

    private static int getRandomY() {
        return (int) (Math.random() * 100);
    }
}
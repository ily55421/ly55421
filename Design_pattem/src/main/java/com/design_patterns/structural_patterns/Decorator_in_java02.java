package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 16:08
 * @Description TODO  Decorator design pattern
 * Create a "lowest common denominator" that makes classes interchangeable
 * Create a second level base class for optional functionality
 * "Core" class and "Decorator" class declare an "isa" relationship
 * Decorator class "has a" instance of the "lowest common denominator"
 * Decorator class delegates to the "has a" object
 * Create a Decorator derived class for each optional embellishment
 * Decorator derived classes delegate to base class AND add extra stuff
 * Client has the responsibility to compose desired configurations
 *
 * 装饰器设计模式
 *
 * 创建一个使类可互换的“最小公分母”
 *
 * 为可选功能创建第二级基类
 *
 * “Core”类和“Decorator”类声明“isa”关系
 *
 * 装饰器类“具有”“最小公分母”的实例
 *
 * 装饰器类委托给“拥有”对象
 *
 * 为每个可选点缀创建一个 Decorator 派生类
 *
 * 装饰器派生类委托给基类并添加额外的东西
 *
 * 客户有责任组成所需的配置
 *
 */
public class Decorator_in_java02 {
    public static void main(String[] args) {
        // 8. Client has the responsibility to compose desired configurations
        Widget02 widget = new BorderDecorator02(new BorderDecorator02(new ScrollDecorator02(new TextField(80, 24))));
        widget.draw();
    }
}
// 1. "lowest common denominator"
interface Widget02 {
    void draw();
}

// 3. "Core" class with "is a" relationship
class TextField implements Widget02 {
    private int width, height;

    public TextField(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public void draw() {
        System.out.println("TextField: " + width + ", " + height);
    }
}

// 2. Second level base class with "isa" relationship
abstract class Decorator02 implements Widget02 {
    // 4. "has a" relationship
    private Widget02 widget;

    public Decorator02(Widget02 widget) {
        this.widget = widget;
    }

    // 5. Delegation
    public void draw() {
        widget.draw();
    }
}

// 6. Optional embellishment
class BorderDecorator02 extends Decorator02 {
    public BorderDecorator02(Widget02 widget) {
        super(widget);
    }
    public void draw() {
        // 7. Delegate to base class and add extra stuff
        super.draw();
        System.out.println("  BorderDecorator02");
    }
}

// 6. Optional embellishment
class ScrollDecorator02 extends Decorator02 {
    public ScrollDecorator02(Widget02 widget) {
        super(widget);
    }
    public void draw() {
        super.draw(); // 7. Delegate to base class and add extra stuff
        System.out.println("  ScrollDecorator02");
    }
}


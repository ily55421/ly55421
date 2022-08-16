package com.design_patterns.structural_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 14:54
 * @Description TODO Facade design pattern
 * Identify the desired unified interface for a set of subsystems
 * Design a "wrapper" class that can encapsulate the use of the subsystems
 * The client uses (is coupled to) the Facade
 * The facade/wrapper "maps" to the APIs of the subsystems
 * <p>
 * 门面设计模式
 * <p>
 * 确定一组子系统所需的统一接口
 * <p>
 * 设计一个可以封装子系统使用的“包装器”类
 * <p>
 * 客户端使用（耦合到）Facade
 * <p>
 * 外观/包装器“映射”到子系统的 API
 */
public class Facade_in_java {
    public static void main(String[] args) {
        // 3. Client uses the Facade
        Line022 lineA = new Line022(new Point02(2, 4), new Point02(5, 7));
        lineA.move(-2, -4);
        System.out.println("after move:  " + lineA);
        // 旋转角度
        lineA.rotate(45);
        System.out.println("after rotate: " + lineA);
        Line022 lineB = new Line022(new Point02(2, 1), new Point02(2.866, 1.5));
        // 旋转角度
        lineB.rotate(30);
        System.out.println("30 degrees to 60 degrees: " + lineB);
        //after move:  origin is (0.0,0.0), end is (3.0,3.0)
        //  Point02Polar is [4.242640687119285@90.0]
        //after rotate: origin is (0.0,0.0), end is (2.5978681687064796E-16,4.242640687119285)
        //  Point02Polar is [0.9999779997579947@60.000727780827376]
        //30 degrees to 60 degrees: origin is (2.0,1.0), end is (2.499977999677324,1.8660127018922195)
    }

}

// 1. Subsystem
class Point02Cartesian {
    private double x, y;

    public Point02Cartesian(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int x, int y) {
        this.x += x;
        this.y += y;
    }

    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}

// 1. Subsystem
class Point02Polar {
    private double radius, angle;

    public Point02Polar(double radius, double angle) {
        this.radius = radius;
        this.angle = angle;
    }

    public void rotate(int angle) {
        this.angle += angle % 360;
    }

    public String toString() {
        return "[" + radius + "@" + angle + "]";
    }
}

// 1. Desired interface: move(), rotate()
class Point02 {
    // 2. Design a "wrapper" class
    private Point02Cartesian pointCartesian;

    public Point02(double x, double y) {
        pointCartesian = new Point02Cartesian(x, y);
    }

    public String toString() {
        return pointCartesian.toString();
    }

    // 4. Wrapper maps
    public void move(int x, int y) {
        pointCartesian.move(x, y);
    }

    public void rotate(int angle, Point02 o) {
        double x = pointCartesian.getX() - o.pointCartesian.getX();
        double y = pointCartesian.getY() - o.pointCartesian.getY();
        Point02Polar pointPolar = new Point02Polar(Math.sqrt(x * x + y * y), Math.atan2(y, x) * 180 / Math.PI);
        // 4. Wrapper maps
        pointPolar.rotate(angle);
        System.out.println("  Point02Polar is " + pointPolar);
        String str = pointPolar.toString();
        int i = str.indexOf('@');
        double r = Double.parseDouble(str.substring(1, i));
        double a = Double.parseDouble(str.substring(i + 1, str.length() - 1));
        pointCartesian = new Point02Cartesian(r * Math.cos(a * Math.PI / 180) + o.pointCartesian.getX(),
                r * Math.sin(a * Math.PI / 180) + o.pointCartesian.getY());
    }
}

class Line022 {
    private Point02 o, e;

    public Line022(Point02 ori, Point02 end) {
        o = ori;
        e = end;
    }

    public void move(int x, int y) {
        o.move(x, y);
        e.move(x, y);
    }

    public void rotate(int angle) {
        e.rotate(angle, o);
    }

    public String toString() {
        return "origin is " + o + ", end is " + e;
    }
}

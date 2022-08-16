package com.design_patterns.structural_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/11 14:40
 * @Description TODO Identify the desired interface.
 * Design a "wrapper" class that can "impedance match" the old to the new.
 * The adapter/wrapper class "has a" instance of the legacy class.
 * The adapter/wrapper class "maps" (or delegates) to the legacy object.
 * The client uses (is coupled to) the new interface.
 * 确定所需的接口。
 * <p>
 * 设计一个“包装”类，可以“阻抗匹配”旧的和新的。
 * <p>
 * 适配器/包装类“有一个”遗留类的实例。
 * <p>
 * 适配器/包装类“映射”（或委托）到遗留对象。
 * <p>
 * 客户端使用（耦合到）新接口。
 */
public class Adapter_in_Java_Another {
    public static void main(String[] args) {
        RoundHole roundHole = new RoundHole(5);
        SquarePegAdapter squarePegAdapter;
        for (int i = 6; i < 10; i++) {
            squarePegAdapter = new SquarePegAdapter((double) i);
            // The client uses (is coupled to) the new interface
            squarePegAdapter.makeFit(roundHole);
            //RoundHole: max SquarePeg is 7.0710678118654755
            //reducing SquarePeg 6.0 by 0.0 amount
            //reducing SquarePeg 7.0 by 0.0 amount
            //reducing SquarePeg 8.0 by 0.9289321881345245 amount
            //   width is now 7.0710678118654755
            //reducing SquarePeg 9.0 by 1.9289321881345245 amount
            //   width is now 7.0710678118654755
        }
    }
}

/* The OLD */
class SquarePeg {
    private double width;

    public SquarePeg(double width) {
        this.width = width;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }
}

/* The NEW */
class RoundHole {
    private final int radius;

    public RoundHole(int radius) {
        this.radius = radius;
        System.out.println("RoundHole: max SquarePeg is " + radius * Math.sqrt(2));
    }

    public int getRadius() {
        return radius;
    }
}

// Design a "wrapper" class that can "impedance match" the old to the new 方钉适配器
class SquarePegAdapter {
    // The adapter/wrapper class "has a" instance of the legacy class “有一个”遗留类的实例
    private final SquarePeg squarePeg;

    public SquarePegAdapter(double w) {
        squarePeg = new SquarePeg(w);
    }

    /**
     * 计算方钉 减少的比例
     *
     * @param roundHole
     */
    // Identify the desired interface  确定所需的接口
    public void makeFit(RoundHole roundHole) {
        // The adapter/wrapper class delegates to the legacy object   返回double值的正确舍入正平方根。 1.41
        double amount = squarePeg.getWidth() - roundHole.getRadius() * Math.sqrt(2);
        System.out.println("reducing SquarePeg " + squarePeg.getWidth() + " by " + ((amount < 0) ? 0 : amount) + " amount");
        if (amount > 0) {
            squarePeg.setWidth(squarePeg.getWidth() - amount);
            System.out.println("   width is now " + squarePeg.getWidth());
        }
    }
}

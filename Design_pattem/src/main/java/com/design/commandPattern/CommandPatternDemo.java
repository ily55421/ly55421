package com.design.commandPattern;


public class CommandPatternDemo {
    public static void main(String[] args) {
        Stock abcStock = new Stock();

        BuyStock buyStockOrder = new BuyStock(abcStock);
        SellStock sellStockOrder = new SellStock(abcStock);
        //添加任务订单
        Broker broker = new Broker();
        broker.takeOrder(buyStockOrder);
        broker.takeOrder(sellStockOrder);
        //下订单  执行列表中的所有对象
        broker.placeOrders();
        //Stock [ Name: ABC, Quantity: 10 ] bought
        //Stock [ Name: ABC, Quantity: 10 ] sold
    }
}
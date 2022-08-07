package com.design.iteratorPattern;

public class IteratorPatternDemo {

    public static void main(String[] args) {
        NameRepository namesRepository = new NameRepository();

        // iter.hasNext() 下一个对象不为空
        for (Iterator iter = namesRepository.getIterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            System.out.println("Name : " + name);
        }
        //Name : Robert
        //Name : John
        //Name : Julie
        //Name : Lora
    }
}
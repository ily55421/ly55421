package com.design.strategyPattern;

/**
 * @author lin 2022/8/8 22:08
 */
public class TemplatePatternDemo {
    public static void main(String[] args) {
        //根据类型生成不同的对象
        Game game = new Cricket();
        game.play();
        System.out.println();
        game = new Football();
        game.play();
        //Cricket Game Initialized! Start playing.
        //Cricket Game Started. Enjoy the game!
        //Cricket Game Finished!
        //
        //Football Game Initialized! Start playing.
        //Football Game Started. Enjoy the game!
        //Football Game Finished!
    }
}

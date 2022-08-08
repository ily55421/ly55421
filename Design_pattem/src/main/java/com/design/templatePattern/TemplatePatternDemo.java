package com.design.templatePattern;

/**
 * @author lin 2022/8/9 1:21
 */
public class TemplatePatternDemo {
    public static void main(String[] args) {

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

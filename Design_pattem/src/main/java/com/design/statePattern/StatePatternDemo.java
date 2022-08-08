package com.design.statePattern;

/**
 * @author lin 2022/8/8 21:59
 */
public class StatePatternDemo {
    public static void main(String[] args) {
        Context context = new Context();

        StartState startState = new StartState();
        //执行  更新状态
        startState.doAction(context);

        System.out.println(context.getState().toString());

        StopState stopState = new StopState();

        stopState.doAction(context);

        System.out.println(context.getState().toString());

        //Player is in start state
        //Start State
        //Player is in stop state
        //Stop State
    }
}

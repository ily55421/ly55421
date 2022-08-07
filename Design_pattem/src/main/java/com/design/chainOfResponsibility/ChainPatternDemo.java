package com.design.chainOfResponsibility;

/**
 * 创建不同类型的记录器
 * 赋予它们不同的错误级别，并在每个记录器中设置下一个记录器
 * 每个记录器中的下一个记录器代表的是链的一部分
 */
public class ChainPatternDemo {
    /**
     * 获取责任链对象 机制
     * @return logger处理对象
     */
    private static AbstractLogger getChainOfLoggers() {

        AbstractLogger errorLogger = new ErrorLogger(AbstractLogger.ERROR);
        AbstractLogger fileLogger = new FileLogger(AbstractLogger.DEBUG);
        AbstractLogger consoleLogger = new ConsoleLogger(AbstractLogger.INFO);

        errorLogger.setNextLogger(fileLogger);
        fileLogger.setNextLogger(consoleLogger);

        return errorLogger;
    }

    public static void main(String[] args) {
        AbstractLogger loggerChain = getChainOfLoggers();

        loggerChain.logMessage(AbstractLogger.INFO,
                "This is an information.");

        loggerChain.logMessage(AbstractLogger.DEBUG,
                "This is an debug level information.");

        loggerChain.logMessage(AbstractLogger.ERROR,
                "This is an error information.");

        //Standard Console::Logger: This is an information.
        //File::Logger: This is an debug level information.
        //Standard Console::Logger: This is an debug level information.
        //Error Console::Logger: This is an error information.
        //File::Logger: This is an error information.
        //Standard Console::Logger: This is an error information.
    }
}

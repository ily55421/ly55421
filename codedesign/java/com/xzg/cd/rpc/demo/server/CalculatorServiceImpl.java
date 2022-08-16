package com.xzg.cd.rpc.demo.server;

/**
 * @author 84168
 */
public class CalculatorServiceImpl implements CalculatorService {

  @Override
  public int add(int a, int b) {
    return a + b;
  }
}

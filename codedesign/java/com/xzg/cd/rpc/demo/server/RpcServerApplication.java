package com.xzg.cd.rpc.demo.server;

import com.xzg.cd.rpc.RpcServer;

/**
 * @author 84168
 */
public class RpcServerApplication {

    public static void main(String[] args) throws Exception {
      CalculatorService service = new CalculatorServiceImpl();
      RpcServer server = new RpcServer();
      server.export(service, 1234);
    }

}

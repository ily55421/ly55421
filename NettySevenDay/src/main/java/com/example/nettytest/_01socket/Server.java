package com.example.nettytest._01socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lin 2022/8/8 23:15
 */
public class Server {
    private final static int port = 8080;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    /**
     * socket 阻塞方法启动
     *
     * @throws IOException
     */
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("接受一个请求");
            new Thread((new Runnable() {
                private Socket mySocket;

                public Runnable setSocket(Socket socket) {
                    this.mySocket = socket;
                    return this;
                }

                @Override
                public void run() {
                    InputStream in = null;
                    byte[] buf = new byte[100];
                    try {
                        in = this.mySocket.getInputStream();
                        //一直读取输入
                        while (true) {
                            int len = in.read(buf);
                            System.out.println(new String(buf, 0, len));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).setSocket(socket)).start();


        }


    }
}

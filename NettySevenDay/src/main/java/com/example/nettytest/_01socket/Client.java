package com.example.nettytest._01socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author lin 2022/8/8 23:14
 */
public class Client {

    /**
     * 客户端连接
     *
     * @param id 用户标识
     * @throws IOException
     * @throws InterruptedException
     */
    public static void conn(String id) throws IOException, InterruptedException {
        int port = 8080;
        String ip = "127.0.0.1";
        Socket socket = new Socket(ip, port);
        new Thread((new Runnable() {
            private Socket mySocket;

            public Runnable setSocket(Socket socket) {
                this.mySocket = socket;
                return this;
            }

            @Override
            public void run() {
                try {
                    OutputStream out = mySocket.getOutputStream();
                    out.write(id.getBytes());
                    out.flush();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).setSocket(socket)).start();

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            try {
                conn("currUserID:" + i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

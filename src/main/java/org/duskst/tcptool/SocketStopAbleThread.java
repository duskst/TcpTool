package org.duskst.tcptool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * socket可关闭线程
 * @date 2017年12月2日
 * @author duskst
 **/
public class SocketStopAbleThread implements Runnable {

    private final String paramString;
    private ServerSocket serverSocket;
    private final HandlerSocket handlerSocket;
    private Thread listenThread;

    private boolean alive = false;

    public SocketStopAbleThread(int port, HandlerSocket handlerSocket, String paramString) {

        this.paramString = paramString;
        this.handlerSocket = handlerSocket;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    private void start() {
        alive = true;
        listenThread = new Thread(this, paramString);
        listenThread.start();
    }

    @Override
    public void run() {
        try {
            while (alive) {
                Socket client = serverSocket.accept();
                handlerSocket.process(client);
            }
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName() + " closed. err");
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    System.out.println("关闭socket错误");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 关闭资源
     */
    public void shutDownSocketAndListenThread() {
        alive = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }
    }
}

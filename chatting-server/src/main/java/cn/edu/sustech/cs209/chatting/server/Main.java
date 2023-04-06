package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) throws IOException {

        // server的端口号为9999
        ServerSocket serverSocket = new ServerSocket(9999);
        // ArrayList<Socket> observer = new ArrayList<>();
        System.out.println("Starting server");

        new Thread(
                () -> {
                    while (true) {
                        // 不断监听有没有新的client建立
                        Socket s = null;
                        try {
                            s = serverSocket.accept();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Client started and the client is " + s);
                        ServerService service = new ServerService(s);
                        // observer.add(s);
                        Thread thread = new Thread(service);
                        thread.start();
                    }
                }
        ).start();

        // 监听client有没有发信息给server，发了就做转发的操作

    }
}

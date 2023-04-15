package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {


    public static void main(String[] args) throws IOException {

        // server的端口号为9999
        ServerSocket serverSocket = new ServerSocket(9999);
        ConcurrentHashMap<String, ServerService> observer = new ConcurrentHashMap<>();
        List<String> actives = new ArrayList<>();
        System.out.println("Starting server");

        while (true) {
            // 不断监听有没有新的client建立
            Socket newClient = null;
            try {
                newClient = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Client started and the client is " + newClient);
            ServerService service = new ServerService(newClient, observer,actives);
            Thread thread = new Thread(service);
            thread.start();

        }
    }
}

package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServerService implements Runnable {
    private Socket client;
    private Scanner in;
    private PrintWriter out;


    public ServerService(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            try {
                in = new Scanner(client.getInputStream());
                out = new PrintWriter(client.getOutputStream());
                doService();
            } finally {
                client.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void doService() {
        while (true) {
            if (!in.hasNext()) return;
            String s = in.next();
            // 处理一下就行了
            System.out.println(s);
        }
    }

}

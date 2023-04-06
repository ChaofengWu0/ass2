package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class test {
    public static void main(String[] args) throws IOException {
        final int serverPort = 9999;
        Socket client = new Socket("localhost", serverPort);
        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();
        Scanner in = new Scanner(is);
        PrintWriter out = new PrintWriter(os);
        out.println("客户端发送内容...");
        out.flush();
    }
}

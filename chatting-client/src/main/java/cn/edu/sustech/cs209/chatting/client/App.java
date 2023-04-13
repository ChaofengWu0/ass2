package cn.edu.sustech.cs209.chatting.client;

import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        String sendToUsername = "1, 2, 3... (3)";
        String[] sendToUsrArr = sendToUsername.split(",");
        for (int i = 0; i < sendToUsrArr.length; i++) {
            if (i == sendToUsrArr.length - 1) {
                int index = sendToUsrArr[i].indexOf("...");
                sendToUsrArr[i] = sendToUsrArr[i].substring(0, index).trim();
                System.out.println(i + " " + sendToUsrArr[i]);
            } else {
                sendToUsrArr[i] = sendToUsrArr[i].trim();
                System.out.println(i + " " + sendToUsrArr[i]);
            }
        }
    }
}
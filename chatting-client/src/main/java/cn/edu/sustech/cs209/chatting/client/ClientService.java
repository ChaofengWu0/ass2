package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientService implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Integer type;

    private Boolean login;
    private String username;
    private List<String> actives;

    private boolean flag1;

    public ClientService(Socket socket) {
        this.socket = socket;
    }

    public ClientService(Socket socket, String username) {
        this.socket = socket;
        // this.username = username;
        String test = username;
        this.actives = new ArrayList<>();
        this.login = false;
        this.flag1 = false;
    }

    @Override
    public void run() {
        try {
            try {
                in = new ObjectInputStream(this.socket.getInputStream());
                out = new ObjectOutputStream(this.socket.getOutputStream());
                System.out.println("clientService runs...");
                doClientService();
            } finally {
                socket.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void doClientService() throws IOException, ClassNotFoundException {
        while (true) {
            // if (!flag1) {
            //     check();
            //     flag1 = true;
            // }
            Object obj = in.readObject();
            if (obj == null) continue;
            Message message = (Message) obj;
            int type = message.getType();
            switch (type) {
                case 1: {
                    System.out.println("client case 1");
                    whetherLogin(message);
                    break;
                }
                case 3: {
                    System.out.println("client case 3");
                    // 获取到了目前有多少人在线
                    this.actives = message.getActives();
                    break;
                }
            }
        }
    }

    public void check() throws IOException {
        System.out.println("do check");
        // 发送给服务器，让服务器去查询当前username有没有在数据库中
        Message handshaking1 = new Message(0, this.username);
        out.writeObject(handshaking1);
        out.flush();
    }

    public void whetherLogin(Message message) throws IOException {
        if (message.getData().equals("success")) {
            this.login = true;
            System.out.println("send howMany message");
            // 要查询一下有多少个人
            Message howMany = new Message(2, "case2");
            out.writeObject(howMany);
            out.flush();
        } else {
            System.out.println("你的账号已在其他地方登录或密码错误");
        }
    }


    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<String> getActives() {
        return actives;
    }

    public void setActives(List<String> actives) {
        this.actives = actives;
    }

    public Boolean getLogin() {
        return login;
    }

    public void setLogin(Boolean login) {
        this.login = login;
    }
}

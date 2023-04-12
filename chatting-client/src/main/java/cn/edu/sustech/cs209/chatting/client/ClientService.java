package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.client.Controller;
import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ClientService implements Runnable {
    private final Socket socket;

    private Controller controller;
    // private ReentrantLock searchActiveLock;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Integer type;

    private Boolean flagCheckLogin;

    private Boolean flagSearchActives;
    private Boolean login;
    private String username;
    private List<String> actives;
    List<Message> messageList;
    List<String> chatList;
    List<String> hasLinks;
    ObservableList<String> observableList_chatList;


    public ClientService(Socket socket, String username, ObjectInputStream in, ObjectOutputStream out, ReentrantLock searchActiveLock, Controller controller) {
        this.controller = controller;
        // this.searchActiveLock = searchActiveLock;
        this.socket = socket;
        this.username = username;
        this.in = in;
        this.out = out;
        this.actives = new ArrayList<>();
        this.login = false;
        this.flagCheckLogin = false;
        this.flagSearchActives = false;
        this.hasLinks = new ArrayList<>();
        this.observableList_chatList = FXCollections.observableArrayList(hasLinks);
        this.messageList = new ArrayList<>();
        this.chatList = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            try {
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
            Object obj = in.readObject();
            if (obj == null) continue;
            Message message = (Message) obj;
            int type = message.getType();
            switch (type) {
                case 1: {
                    System.out.println("client case 1");
                    whetherLogin(message);
                    this.flagCheckLogin = true;
                    break;
                }
                case 3: {
                    System.out.println("client case 3");
                    // 获取到了目前有多少人在线
                    getHowManyActives(message);
                    // List<String> list = message.getActives();
                    // Platform.runLater(()-> = list);
                    break;
                }
                case 5: {
                    System.out.println("client case 5");
                    saveMessage(message);
                    // addLinks(message);
                    break;
                }
                // case 7: {
                //     System.out.println("client case 7");
                //     handShaking2(message);
                //     break;
                // }
            }
        }
    }

    public void whetherLogin(Message message) {
        if (message.getData().equals("success")) {
            this.login = true;
        } else {
            this.login = false;
            System.out.println("你的账号已在其他地方登录或密码错误");
        }
    }

    public void getHowManyActives(Message message) {
        this.actives.add(message.getData());
        Platform.runLater(() -> {
            this.controller.currentOnlineCnt.setText(String.valueOf(this.actives.size() + 1));
            controller.setActives(this.actives);
        });
    }

    public synchronized void saveMessage(Message message) {
        this.messageList.add(message);
        if (!this.observableList_chatList.contains(message.getSentBy())) {
            this.observableList_chatList.add(message.getSentBy());
            Platform.runLater(() -> {
                controller.chatList.setItems(observableList_chatList);
            });
            serviceShowMsg(message);
        } else {
            serviceShowMsg(message);
        }
        System.out.println(username + " 现在有 " + this.messageList.size() + "条消息");
    }

    public void serviceShowMsg(Message message) {
        List<Message> tmpMessageList = this.messageList.stream().filter(e -> e.getSendTo().equals(message.getSendTo())
                || e.getSentBy().equals(username)).collect(Collectors.toList());
        ObservableList<Message> observableList = FXCollections.observableArrayList(tmpMessageList);
        Platform.runLater(() -> {
            controller.chatContentList.setItems(observableList);
        });
    }

    public Boolean getFlagSearchActives() {
        return flagSearchActives;
    }

    public void setFlagSearchActives(Boolean flagSearchActives) {
        this.flagSearchActives = flagSearchActives;
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

    public Boolean getFlagCheckLogin() {
        return flagCheckLogin;
    }

    public void setFlagCheckLogin(Boolean flagCheckLogin) {
        this.flagCheckLogin = flagCheckLogin;
    }

}

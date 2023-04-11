package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ClientService implements Runnable {
    private final Socket socket;

    private ReentrantLock searchActiveLock;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Integer type;

    private Boolean flagCheckLogin;

    private Boolean flagSearchActives;
    private Boolean login;
    private String username;
    private List<String> actives;
    List<String> hasLinks;
    ObservableList<String> observableList;

    public ClientService(Socket socket, String username, ObjectInputStream in, ObjectOutputStream out, ReentrantLock searchActiveLock) {
        this.searchActiveLock = searchActiveLock;
        this.socket = socket;
        this.username = username;
        this.in = in;
        this.out = out;
        this.actives = new ArrayList<>();
        this.login = false;
        this.flagCheckLogin = false;
        this.flagSearchActives = false;
        this.hasLinks = new ArrayList<>();
        this.observableList = FXCollections.observableArrayList(hasLinks);

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

    public synchronized void doClientService() throws IOException, ClassNotFoundException {
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
                    // for (int i = 0; i < list.size(); i++) {
                    //     System.out.println(list.get(i));
                    // }
                    // Platform.runLater(()->Controller.this.actives = list);
                    break;
                }
                case 5: {
                    System.out.println("client case 5");
                    addLinks(message);
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
        System.out.println(flagSearchActives);
        System.out.println("before set flag");
        searchActiveLock.lock();
        this.setActives(message.getActives());
        this.setFlagSearchActives(true);
        System.out.println("after flag");
        System.out.println("getHowManyActives : " + flagSearchActives);
        searchActiveLock.unlock();
    }

    public void addLinks(Message message) {
        this.hasLinks.add(message.getData());
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

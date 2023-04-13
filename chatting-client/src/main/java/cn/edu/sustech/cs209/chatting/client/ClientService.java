package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService implements Runnable {
    private final Socket socket;

    private Controller controller;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Integer type;

    private Boolean flagCheckLogin;

    private Boolean flagSearchActives;
    private Boolean login;
    private String username;
    private String selectedUsr;

    private List<Controller.ChatObj> actives;
    List<Message> messageList;
    List<Controller.ChatObj> chatList;
    // List<String> hasLinks;
    ObservableList<Controller.ChatObj> observableList_chatListPrivate_chatObj;
    ObservableList<Controller.ChatObj> observableList_chatListGroup;
    ObservableList<String> observableList_chatListPrivate;
    // ObservableList<Controller.ChatObj> observableList_chatListGroup_show;
    ObservableList<String> observableList_chatListGroup_show_StringList;
    HashMap<String, Controller.ChatObj> observableList_chatListGroup_hashmap;


    public ClientService(Socket socket, String username, ObjectInputStream in, ObjectOutputStream out, Controller controller, String sendTo) {
        this.controller = controller;
        // this.searchActiveLock = searchActiveLock;
        this.socket = socket;
        this.username = username;
        this.in = in;
        this.out = out;
        this.actives = new ArrayList<>();
        this.login = false;
        this.flagSearchActives = false;
        // this.hasLinks = new ArrayList<>();
        this.observableList_chatListPrivate_chatObj = FXCollections.observableArrayList();
        this.observableList_chatListGroup = FXCollections.observableArrayList();
        this.observableList_chatListGroup_hashmap = new HashMap<>();
        // this.observableList_chatListGroup_show = FXCollections.observableArrayList();
        this.observableList_chatListPrivate = FXCollections.observableArrayList();
        this.observableList_chatListGroup_show_StringList = FXCollections.observableArrayList();
        this.messageList = new ArrayList<>();
        this.chatList = new ArrayList<>();
        this.selectedUsr = sendTo;
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
        while (!Thread.currentThread().isInterrupted()) {
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
                    getHowManyActives(message);
                    // List<String> list = message.getActives();
                    // Platform.runLater(()-> = list);
                    break;
                }
                case 5: {
                    System.out.println("client case 5");
                    saveMessage(message);
                    break;
                }
                // case 7: {
                //     System.out.println("client case 7");
                //     saveMessageGroup(message);
                //     break;
                // }
            }
        }
        System.out.println("interrupted");
    }

    public void whetherLogin(Message message) {
        if (message.getData().equals("success")) {
            System.out.println(message.getData());
            this.login = true;
        } else {
            System.out.println(message.getData());
            this.login = false;
            System.out.println("你的账号已在其他地方登录或密码错误");
        }
        this.flagCheckLogin = true;
    }

    public void getHowManyActives(Message message) {
        this.actives.add(new Controller.ChatObj(message.getData(), message.getData()));
        Platform.runLater(() -> {
            this.controller.currentOnlineCnt.setText(String.valueOf(this.actives.size() + 1));
            controller.setActives(this.actives);
        });
    }

    public synchronized void saveMessage(Message message) {
        this.messageList.add(message);
        String[] sendToUsrArr = message.getSendTo().split(",");
        if (sendToUsrArr.length == 1) {
            saveMessagePrivate(message);
            return;
        }

        List<String> listForChange = new ArrayList<>();
        for (int i = 0; i < sendToUsrArr.length; i++) {
            sendToUsrArr[i] = sendToUsrArr[i].trim();
            listForChange.add(sendToUsrArr[i]);
        }
        saveMessageGroup(message, listForChange);
    }

    public synchronized void saveMessageGroup(Message message, List<String> listForChange) {
        System.out.println("groupMsg");
        if (!this.observableList_chatListGroup_show_StringList.contains(message.getSendTo())) {
            System.out.println("clientService saveMessageGroup not exists");

            String selectedOptionsToString_show = changeIntoShow(listForChange);
            Controller.ChatObj nowChatObj = new Controller.ChatObj(message.getSendTo()
                    , selectedOptionsToString_show);
            this.observableList_chatListGroup_hashmap.put(message.getSendTo(), nowChatObj);
            this.observableList_chatListGroup.add(nowChatObj);
            this.observableList_chatListGroup_show_StringList.add(message.getSendTo());
            showChatList();
            serviceShowMsg(message);
        } else {
            System.out.println("clientService saveMessageGroup exists");
            serviceShowMsg(message);
        }
        System.out.println(username + " 现在有 " + this.messageList.size() + "条消息");
    }


    public void showChatList() {
        ObservableList<Controller.ChatObj> privateList = observableList_chatListPrivate_chatObj;
        ObservableList<Controller.ChatObj> groupList = observableList_chatListGroup;
        ObservableList<Controller.ChatObj> allList = FXCollections.observableArrayList();
        if (privateList != null) allList.addAll(privateList);
        if (groupList != null) allList.addAll(groupList);
        Platform.runLater(() -> {
            controller.chatList.setItems(allList);
            controller.chatList.setCellFactory(new Controller.ChatListCellFactory());
        });
    }

    public synchronized void saveMessagePrivate(Message message) {
        System.out.println("privateMsg");
        if (!this.observableList_chatListPrivate.contains(message.getSentBy())) {
            Platform.runLater(() -> {
                this.observableList_chatListPrivate_chatObj.add(new Controller.ChatObj(message.getSentBy(), message.getSentBy()));
                this.observableList_chatListPrivate.add(message.getSentBy());
                controller.chatList.setItems(observableList_chatListPrivate_chatObj);
            });
            serviceShowMsg(message);
        } else {
            serviceShowMsg(message);
        }
        System.out.println(username + " 现在有 " + this.messageList.size() + "条消息");
    }


    public void serviceShowMsg(Message message) {
        // 要显示哪些信息？
        if (message.getSendTo().split(",").length == 1) {
            System.out.println("The selected is " + selectedUsr);
            System.out.println("private msg in ShowMsg");
            showMsgPrivate(message);
        } else {
            System.out.println("The selected is " + selectedUsr);
            System.out.println("group msg in ShowMsg");
            showMsgGroup(message);
        }
    }

    public void showMsgPrivate(Message message) {
        // 第一类，“我”收到的,即sendBy==selectedUsr
        // 第二类，“我”发出的，即sendTo==selectedUsr
        List<Message> tmpMessageList = this.messageList.stream().filter(e ->
                        (e.getSendTo().equals(selectedUsr) && e.getSentBy().equals(username))
                                || (e.getSentBy().equals(selectedUsr) && e.getSendTo().equals(username))
                )
                .collect(Collectors.toList());
        ObservableList<Message> observableList = FXCollections.observableArrayList(tmpMessageList);
        Platform.runLater(() -> {
            controller.chatContentList.setItems(observableList);
        });
    }

    public void showMsgGroup(Message message) {
        List<Message> tmpMessageList = this.messageList.stream().filter(e ->
                        (e.getSendTo().equals(message.getSendTo())))
                .collect(Collectors.toList());
        ObservableList<Message> observableList = FXCollections.observableArrayList(tmpMessageList);
        Platform.runLater(() -> {
            controller.chatContentList.setItems(observableList);
        });
    }

    public String changeIntoShow(List<String> selectedOptions) {
        String sb = selectedOptions.get(0) + ", " + selectedOptions.get(1) + ", " + selectedOptions.get(2) +
                String.format("... (%d)", selectedOptions.size());
        return sb;
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

    public List<Controller.ChatObj> getActives() {
        return actives;
    }

    public void setActives(List<Controller.ChatObj> actives) {
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

    public String getSelectedUsr() {
        return selectedUsr;
    }

    public void setSelectedUsr(String selectedUsr) {
        this.selectedUsr = selectedUsr;
    }

    public ObservableList<Controller.ChatObj> getObservableList_chatListPrivate_chatObj() {
        return observableList_chatListPrivate_chatObj;
    }

    public void setObservableList_chatListPrivate_chatObj(ObservableList<Controller.ChatObj> observableList_chatListPrivate_chatObj) {
        this.observableList_chatListPrivate_chatObj = observableList_chatListPrivate_chatObj;
    }

    public ObservableList<Controller.ChatObj> getObservableList_chatListGroup() {
        return observableList_chatListGroup;
    }

    public void setObservableList_chatListGroup(ObservableList<Controller.ChatObj> observableList_chatListGroup) {
        this.observableList_chatListGroup = observableList_chatListGroup;
    }

    public ObservableList<String> getObservableList_chatListGroup_show_StringList() {
        return observableList_chatListGroup_show_StringList;
    }

    public void setObservableList_chatListGroup_show_StringList(ObservableList<String> observableList_chatListGroup_show_StringList) {
        this.observableList_chatListGroup_show_StringList = observableList_chatListGroup_show_StringList;
    }

}

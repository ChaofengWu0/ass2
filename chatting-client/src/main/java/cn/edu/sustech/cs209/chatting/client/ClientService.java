package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class ClientService implements Runnable {
  private final Socket socket;

  private final Controller controller;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private Integer type;

  private Boolean flagCheckLogin;
  private Boolean flagSearchActives;
  private Boolean login;
  private final String username;
  private String selectedUsr;

  private List<Controller.ChatObj> actives;
  List<Message> messageList;
  List<Controller.ChatObj> chatList;
  // List<String> hasLinks;
  ObservableList<Controller.ChatObj> observableList_chatListPrivate_chatObj;
  ObservableList<Controller.ChatObj> observableList_chatListGroup;
  ObservableList<String> observableList_chatListPrivate;
  ObservableList<String> observableList_chatListGroup_ActualData;
  HashMap<String, Controller.ChatObj> observableList_chatListGroup_hashmap;
  HashMap<String, Controller.ChatObj> observableList_chatListPrivate_hashmap;
  HashMap<String, Controller.ChatObj> observableList_chatListPrivate_hashmap_forAccount;


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
    this.observableList_chatListGroup_ActualData = FXCollections.observableArrayList();
    observableList_chatListPrivate_hashmap_forAccount = new HashMap<>();
    this.observableList_chatListPrivate_hashmap = new HashMap<>();
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

  public synchronized void doClientService() throws ClassNotFoundException, IOException {
    while (!Thread.currentThread().isInterrupted()) {
      Object obj = null;
      try {
        System.out.println("clientService before in");
        obj = in.readObject();
        System.out.println("clientService after in");
      } catch (IOException e) {
        this.in.close();
        this.out.close();
        this.socket.close();
        Thread.currentThread().interrupt();
        continue;
      }
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
          getHowManyActives(message);
          break;
        }
        case 5: {
          System.out.println("client case 5");
          saveMessage(message);
          break;
        }
        case 7: {
          System.out.println("client case 7");
          dead(message);
          break;
        }
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
    Controller.ChatObj newObj = new Controller.ChatObj(message.getData(), message.getData());
    this.actives.add(newObj);
    this.observableList_chatListPrivate_hashmap_forAccount.put(newObj.actualData, newObj);
    showActiveList();
  }

  public void dead(Message message) {
    // 接收到某个client死掉了，那么我需要把他从actives里面剔除即可。
    String name = message.getData();
    Controller.ChatObj deleteChatObj = this.observableList_chatListPrivate_hashmap_forAccount.get(name);
    this.actives.remove(deleteChatObj);
    this.observableList_chatListPrivate_hashmap_forAccount.remove(name);
    showActiveList();
  }

  public void showActiveList() {
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

    serviceShowMsg(message);

  }

  public synchronized void saveMessageGroup(Message message, List<String> listForChange) {
    System.out.println("groupMsg");
    if (!this.observableList_chatListGroup_ActualData.contains(message.getSendTo())) {
      System.out.println("clientService saveMessageGroup not exists");

      String selectedOptionsToString_show = changeIntoShow(listForChange);
      Controller.ChatObj nowChatObj = new Controller.ChatObj(message.getSendTo()
              , selectedOptionsToString_show);
      this.observableList_chatListGroup_hashmap.put(message.getSendTo(), nowChatObj);
      this.observableList_chatListGroup.add(nowChatObj);
      this.observableList_chatListGroup_ActualData.add(message.getSendTo());
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
      Controller.ChatObj chatObj = new Controller.ChatObj(message.getSentBy(), message.getSentBy());
      this.observableList_chatListPrivate_chatObj.add(chatObj);
      this.observableList_chatListPrivate.add(message.getSentBy());
      this.observableList_chatListPrivate_hashmap.put(message.getSentBy(), chatObj);
      showChatList();
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
      showMsgPrivate();
    } else {
      System.out.println("The selected is " + selectedUsr);
      System.out.println("group msg in ShowMsg");
      showMsgGroup(message);
    }
  }

  public void showMsgPrivate() {
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
    return selectedOptions.get(0) + ", " + selectedOptions.get(1) + ", " + selectedOptions.get(2) +
            String.format("... (%d)", selectedOptions.size());
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

  public ObservableList<String> getObservableList_chatListGroup_ActualData() {
    return observableList_chatListGroup_ActualData;
  }

  public void setObservableList_chatListGroup_ActualData(ObservableList<String> observableList_chatListGroup_ActualData) {
    this.observableList_chatListGroup_ActualData = observableList_chatListGroup_ActualData;
  }

  public ObjectInputStream getIn() {
    return in;
  }

  public void setIn(ObjectInputStream in) {
    this.in = in;
  }

  public ObjectOutputStream getOut() {
    return out;
  }

  public void setOut(ObjectOutputStream out) {
    this.out = out;
  }

  public Socket getSocket() {
    return socket;
  }
}

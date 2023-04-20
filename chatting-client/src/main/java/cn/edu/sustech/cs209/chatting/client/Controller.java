package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;


public class Controller implements Initializable {
  @FXML
  Label currentUsername;
  @FXML
  Label currentOnlineCnt;
  @FXML
  ListView<ChatObj> chatList;
  @FXML
  ListView<Message> chatContentList;
  @FXML
  GridPane emoji;
  @FXML
  TextArea inputArea;
  Thread son;
  final int serverPort = 9999;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private Socket socket;
  private String username;
  private String sendTo;
  private ClientService clientService;
  private List<ChatObj> actives;
  HashMap<String, byte[]> unicodeToEmoji;


  public void check() throws IOException {
    // 发送给服务器，让服务器去查询当前username有没有在数据库中
    Message check = new Message(0, this.username);
    out.writeObject(check);
    out.flush();
  }

  public void setEmojis() {
    Label emoji1 = new Label("\uD83D\uDE2D");
    Label emoji2 = new Label("\uD83D\uDE22");
    Label emoji3 = new Label("\uD83D\uDE0D");
    Label emoji4 = new Label("\uD83D\uDE1D");
    unicodeToEmoji.put(emoji1.getText(), "\uD83D\uDE2D".getBytes(StandardCharsets.UTF_8));
    unicodeToEmoji.put(emoji2.getText(), "\uD83D\uDE22".getBytes(StandardCharsets.UTF_8));
    unicodeToEmoji.put(emoji3.getText(), "\uD83D\uDE0D".getBytes(StandardCharsets.UTF_8));
    unicodeToEmoji.put(emoji4.getText(), "\uD83D\uDE1D".getBytes(StandardCharsets.UTF_8));
    emoji.add(emoji1, 0, 0);
    emoji.add(emoji2, 0, 1);
    emoji.add(emoji3, 1, 1);
    emoji.add(emoji4, 1, 0);
    GridPane.setHalignment(emoji1, HPos.CENTER);
    GridPane.setValignment(emoji1, VPos.CENTER);
    GridPane.setHalignment(emoji2, HPos.CENTER);
    GridPane.setValignment(emoji2, VPos.CENTER);
    GridPane.setHalignment(emoji3, HPos.CENTER);
    GridPane.setValignment(emoji3, VPos.CENTER);
    GridPane.setHalignment(emoji4, HPos.CENTER);
    GridPane.setValignment(emoji4, VPos.CENTER);
    addEmoji();
  }

  public void addEmoji() {
    for (int i = 0; i < emoji.getChildren().size(); i++) {
      Label nowLabel = (Label) emoji.getChildren().get(i);
      emoji.getChildren().get(i).setOnMouseClicked(event -> {
        byte[] bytes = unicodeToEmoji.get(nowLabel.getText());
        String unicodeString = new String(bytes, 0,
                bytes.length, StandardCharsets.UTF_8); // 将字节数组转换为字符串
        Platform.runLater(() -> {
          inputArea.appendText(unicodeString);
        });
      });
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    this.actives = new ArrayList<>();
    this.unicodeToEmoji = new HashMap<>();
    this.sendTo = "";
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    Optional<String> input = dialog.showAndWait();
    setEmojis();

    if (input.isPresent() && !input.get().isEmpty()) {
      username = input.get();
      /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                if so, ask the user to change the username
      */
      try {
        socket = new Socket("localhost", serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println(socket);
        clientService = new ClientService(socket, username, in, out, this, sendTo);
        this.son = new Thread(clientService);
        this.son.start();
        // handshaking1
        check();
        while (clientService.getFlagCheckLogin() == null) {
          Thread.sleep(10);
        }
        clientService.setFlagCheckLogin(null);
        // login没成功，走这里,要弹出警告
        try {
          if (!clientService.getLogin()) {
            throw new RuntimeException();
          }
        } catch (RuntimeException e) {
          this.son.interrupt();
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setTitle("Warning Dialog");
          alert.setHeaderText(null);
          alert.setContentText("The user has already been online!\nPlease input a user not online!");
          alert.showAndWait();
          Platform.exit();
          initialize(url, resourceBundle);
          System.exit(0);
        }
        currentUsername.setText("Current User: " + username);
        // 成功上线，要发一个message
        loginSuccess();
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
      System.out.println("username: " + username);
    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }

    chatList.getSelectionModel()
            .selectedItemProperty()
            .addListener((ObservableValue<? extends ChatObj> observable, ChatObj oldValue, ChatObj newValue) -> {
                      // 捕获到了，就要把右边的改过去
                      if (newValue != null) {
                        sendTo = newValue.actualData;
                        System.out.println("sendTo has , ? : " + sendTo.contains(","));
                        if (sendTo.contains(",")) {
                          // 说明是多人聊天
                          String[] tmpArr = newValue.actualData.split(",");
                          List<String> list = new ArrayList<>(Arrays.asList(tmpArr));
                          newValue.chatListShow = changeIntoShow(list);
                        } else {
                          // 单人聊天
                          System.out.println("solo private msg");
                          newValue.chatListShow = sendTo;
                        }
                        System.out.println("actualData : " + sendTo);
                      }
                      chatList.setCellFactory(new ChatListCellFactory());
                      this.clientService.setSelectedUsr(sendTo);
                      System.out.println("now sendTo: " + sendTo);
                      showMsg();
                    }
            );

    // 心跳消息
    // whole.setCl
    chatList.setCellFactory(new ChatListCellFactory());
    chatContentList.setCellFactory(new MessageCellFactory());
    heart();
  }

  public void heart() {
    Timer timer = new Timer();
    boolean flag = false;
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        Message heartMsg = new Message(8, "");
        try {
          if (out != null) {
            out.writeObject(heartMsg);
            out.flush();
          }
        } catch (IOException e) {
          Platform.runLater(() -> {
            alertServerExit();
            this.cancel();
            Platform.exit();
          });
        }
      }
    };
    long delay = 0; // 延迟时间，单位为毫秒，0 表示立即执行
    long period = 5000; // 执行周期，单位为毫秒，表示每隔 1 秒执行一次
    timer.schedule(task, delay, period);

  }

  public void loginSuccess() throws IOException {
    Message loginMessage = new Message(2, username);
    out.writeObject(loginMessage);
    out.flush();
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<ChatObj> user = new AtomicReference<>();
    Stage stage = new Stage();
    ComboBox<ChatObj> userSel = new ComboBox<>();
    userSel.setCellFactory(new ComboBoxCellFactory());
    // FIXME: get the user list from server, the current user's name should be filtered out
    for (ChatObj active : actives) {
      userSel.getItems().add(active);
    }
    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      stage.close();
    });


    // 这个box时用来选择的那个东西
    HBox box = new HBox(200);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(30, 30, 30, 30));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();


    // 选了用户并且点了ok之后.
    ChatObj selectedUser = user.get();
    if (selectedUser == null) {
      return;
    }
    if (clientService.observableList_chatListPrivate.contains(selectedUser.actualData)) {
      ChatObj chatObj = this.clientService.observableList_chatListPrivate_hashmap.get(selectedUser.actualData);
      chatList.getSelectionModel().select(chatObj);
      chatList.setCellFactory(new ChatListCellFactory());
      chatContentList.setCellFactory(new MessageCellFactory());
      System.out.println("Already has this link");
    } else {
      clientService.observableList_chatListPrivate.add(selectedUser.actualData);
      clientService.observableList_chatListPrivate_chatObj.add(selectedUser);
      clientService.observableList_chatListPrivate_hashmap.put(selectedUser.actualData, selectedUser);
      showChatList();
    }
  }

  public void showChatList() {
    ObservableList<ChatObj> privateList = clientService.observableList_chatListPrivate_chatObj;
    ObservableList<ChatObj> groupList = clientService.observableList_chatListGroup;
    ObservableList<ChatObj> allList = FXCollections.observableArrayList();
    if (privateList != null) allList.addAll(privateList);
    if (groupList != null) allList.addAll(groupList);
    chatList.setItems(allList);
    chatList.setCellFactory(new ChatListCellFactory());
    chatContentList.setCellFactory(new MessageCellFactory());
  }

  public void kill(Stage stage) {
    stage.setOnCloseRequest(event -> {
      System.out.println("this client will be dead");
      // 发消息，告诉服务器，他死了，
      // 其他人可以继续和他通信吗？
      Message deadMsg = new Message(6, username);
      try {
        out.writeObject(deadMsg);
        out.flush();
        Platform.exit();
        closeAll();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      // 退出程序
      System.exit(0);
    });
  }

  public void closeAll() throws IOException {
    this.out.close();
    this.in.close();
    this.socket.close();
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name.
   * You can select several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat:
   * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
   * UserA, UserB, UserC... (10)
   * If there are <= 3 users: do not display the ellipsis, for example:
   * UserA, UserB (2)
   */

  public void alertActivesNotEnough() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Warning Dialog");
    alert.setHeaderText(null);
    alert.setContentText("The number of actives is not enough, you should choose to start a private chat.");
    alert.showAndWait();
  }

  public void alertSelectedNotEnough() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Warning Dialog");
    alert.setHeaderText(null);
    alert.setContentText("The number of selected is not enough, you should choose to start a private chat or choose more users to chat.");
    alert.showAndWait();
  }

  public void alertServerExit() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Warning Dialog");
    alert.setHeaderText(null);
    alert.setContentText("The server is dead!! And you should send no more info");
    alert.showAndWait();
  }

  @FXML
  public void createGroupChat() {
    Stage stage = new Stage();

    ComboBox<CheckBox> userSel = new ComboBox<>();
    ArrayList<CheckBox> userSelArr = new ArrayList<>();

    for (ChatObj active : actives) {
      userSelArr.add(new CheckBox(active.actualData));
    }
    System.out.println("userSelArr size : " + userSelArr.size());
    if (userSelArr.size() < 2) {
      alertActivesNotEnough();
      stage.close();
      return;
    }

    userSel.setItems(FXCollections.observableList(userSelArr));
    List<String> selectedOptions = new ArrayList<>();
    selectedOptions.add(username);
    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      for (CheckBox checkBox : userSel.getItems()) {
        if (checkBox.isSelected()) {
          selectedOptions.add(checkBox.getText());
        }
      }
      System.out.println("Selected options: " + selectedOptions);
      stage.close();
    });

    HBox box = new HBox(200);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(30, 30, 30, 30));
    box.getChildren().addAll(userSelArr);
    box.getChildren().addAll(okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    if (selectedOptions.size() == 1) {
      return;
    }

    // 选了用户并且点了ok之后.
    if (selectedOptions.size() <= 2) {
      alertSelectedNotEnough();
      return;
    }
    // 排序
    Collections.sort(selectedOptions);

    String selectedOptionsToString = changeIntoString(selectedOptions);
    String selectedOptionsToString_show = changeIntoShow(selectedOptions);
    if (this.clientService.observableList_chatListGroup_ActualData.contains(selectedOptionsToString)) {
      // 怎么打开这个对话框.
      ChatObj nowObj = this.clientService.observableList_chatListGroup_hashmap.get(selectedOptionsToString);
      chatList.getSelectionModel().select(nowObj);
      chatList.setCellFactory(new ChatListCellFactory());
      chatContentList.setCellFactory(new MessageCellFactory());
      System.out.println("Already has this link!");
    } else {
      ChatObj nowObj = new ChatObj(selectedOptionsToString, selectedOptionsToString_show);
      this.clientService.observableList_chatListGroup_ActualData.add(selectedOptionsToString);
      this.clientService.observableList_chatListGroup_hashmap.put(selectedOptionsToString, nowObj);
      this.clientService.observableList_chatListGroup.add(nowObj);
      showChatList();
    }
  }

  public String changeIntoString(List<String> selectedOptions) {
    StringBuilder sb = new StringBuilder();
    for (String selectedOption : selectedOptions) {
      sb.append(selectedOption).append(",").append(" ");
    }
    sb.setLength(sb.length() - 2);
    return sb.toString();
  }

  public String changeIntoShow(List<String> selectedOptions) {
    return selectedOptions.get(0) + ", " + selectedOptions.get(1) + ", " + selectedOptions.get(2) +
            String.format("... (%d)", selectedOptions.size());
  }


  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed.
   * After sending the message, you should clear the text input field.
   */
  @FXML
  public synchronized void doSendMessage() throws IOException {
    // TODO
    if (sendTo == null) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Warning Dialog");
      alert.setHeaderText(null);
      alert.setContentText("You should select a user to send your msg");
      alert.showAndWait();
    }
    String data = inputArea.getText();
    if (data == null || data.trim().isEmpty()) {
      inputArea.clear();
      System.out.println("空格");
      return;
    }
    /**
     * 上面的是对输入的内容进行检查
     */
    System.out.println("in showChatList, and for now the one selected is " + chatList.getSelectionModel().getSelectedItem());
    Long nowTime = System.currentTimeMillis();
    Message sendPrivateMessage = new Message(nowTime, username, sendTo, data, 4);
    this.clientService.messageList.add(sendPrivateMessage);
    out.writeObject(sendPrivateMessage);
    out.flush();
    showMsg();
    this.chatContentList.setCellFactory(new MessageCellFactory());
    inputArea.clear();
  }


  public synchronized void showMsg() {
    // 下面的是处理信息的，并且显示在右上方
    if (sendTo.split(",").length == 1) {
      System.out.println("Controller private msg");
      List<Message> tmpMessageList = this.clientService.messageList.stream().filter(e ->
              (e.getSentBy().equals(sendTo) && e.getSendTo().equals(username))
                      || (e.getSendTo().equals(sendTo) && e.getSentBy().equals(username))
      ).collect(Collectors.toList());
      ObservableList<Message> messageObservableList = FXCollections.observableArrayList(tmpMessageList);
      this.chatContentList.setItems(messageObservableList);
    } else {
      // 群发消息显示
      List<Message> tmpMessageList = clientService.messageList.stream().filter(e ->
                      (e.getSendTo().equals(sendTo)))
              .collect(Collectors.toList());
      ObservableList<Message> observableList = FXCollections.observableArrayList(tmpMessageList);

      chatContentList.setItems(observableList);
      System.out.println("Controller group msg");
    }
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model.
   * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          msgLabel.setFont(Font.font("Noto Color Emoji"));
          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }

  static class ChatListCellFactory implements Callback<ListView<ChatObj>, ListCell<ChatObj>> {
    @Override
    public ListCell<ChatObj> call(ListView<ChatObj> param) {
      return new ListCell<ChatObj>() {
        @Override
        protected void updateItem(ChatObj item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
          } else {
            setText(item.getChatListShow());
          }
        }
      };
    }
  }

  private class ComboBoxCellFactory implements Callback<ListView<ChatObj>, ListCell<ChatObj>> {
    @Override
    public ListCell<ChatObj> call(ListView<ChatObj> param) {
      return new ListCell<ChatObj>() {
        @Override
        protected void updateItem(ChatObj item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
          } else {
            setText(item.getChatListShow());
          }
        }
      };
    }
  }

  public String getSendTo() {
    return sendTo;
  }

  public void setSendTo(String sendTo) {
    this.sendTo = sendTo;
  }

  public List<ChatObj> getActives() {
    return actives;
  }

  public void setActives(List<ChatObj> actives) {
    this.actives = actives;
  }

  public static class ChatObj {
    String chatListShow;
    String actualData;

    public ChatObj(String actualData, String chatListShow) {
      this.actualData = actualData;
      this.chatListShow = chatListShow;
    }

    public String getChatListShow() {
      return chatListShow;
    }

    public void setChatListShow(String chatListShow) {
      this.chatListShow = chatListShow;
    }

    public String getActualData() {
      return actualData;
    }

    public void setActualData(String actualData) {
      this.actualData = actualData;
    }

    @Override
    public String toString() {
      return actualData;
    }
  }

}

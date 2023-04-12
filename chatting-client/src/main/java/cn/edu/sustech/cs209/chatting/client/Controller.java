package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    Label currentUsername;

    @FXML
    Label currentOnlineCnt;

    @FXML
    ListView<String> chatList;

    @FXML
    ListView<Message> chatContentList;

    @FXML
    TextArea inputArea;

    private List<String> selectedUsers;
    final int serverPort = 9999;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private String username;
    private String sendTo;
    private ClientService clientService;

    // private ObservableList<String> chatArrayList;
    private List<String> actives;


    public void check() throws IOException {
        // 发送给服务器，让服务器去查询当前username有没有在数据库中
        Message check = new Message(0, this.username);
        out.writeObject(check);
        out.flush();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.actives = new ArrayList<>();
        this.selectedUsers = new ArrayList<>();
        // this.chatArrayList = FXCollections.observableArrayList();
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");
        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            username = input.get();
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            try {
                socket = new Socket("localhost", serverPort);
                System.out.println("in");
                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("out");
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println(socket);
                clientService = new ClientService(socket, username, in, out, new ReentrantLock(), this);
                Thread thread = new Thread(clientService);
                thread.start();
                // handshaking1
                check();
                while (clientService.getFlagCheckLogin() == null) {

                }
                // login没成功，走这里,要弹出警告
                if (!clientService.getLogin()) {
                    // 弹出警告
                }
                currentUsername.setText("Current User: " + username);
                // 成功上线，要发一个message
                loginSuccess();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("username: " + username);
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatList.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, oldValue, newValue) -> {
                            // 捕获到了，就要把右边的改过去
                            sendTo = newValue;
                            showMsg();
                        }
                );
        chatContentList.setCellFactory(new MessageCellFactory());
    }

    public void loginSuccess() throws IOException {
        Message loginMessage = new Message(2, username);
        out.writeObject(loginMessage);
        out.flush();
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();
        // FIXME: get the user list from server, the current user's name should be filtered out
        for (String active : actives) {
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
        String selectedUser = user.get();
        if (clientService.observableList_chatList.contains(selectedUser)) {
            System.out.println("已经有这个链接了");
            // 打开就好
        } else {
            clientService.observableList_chatList.add(selectedUser);
            chatList.setItems(clientService.observableList_chatList);
        }
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
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
    @FXML
    public void createGroupChat() {
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
        String data = inputArea.getText();
        Long nowTime = System.currentTimeMillis();
        Message sendPrivateMessage = new Message(nowTime, username, sendTo, data, 4);
        this.clientService.messageList.add(sendPrivateMessage);
        System.out.println(username + " 现在有 " + this.clientService.messageList.size() + "条消息");
        out.writeObject(sendPrivateMessage);
        out.flush();
        showMsg();
        this.chatContentList.setCellFactory(new MessageCellFactory());
        inputArea.clear();
    }


    public synchronized void showMsg() {
        // 下面的是处理信息的，并且显示在右上方
        List<Message> tmpMessageList = this.clientService.messageList.stream().filter(e -> e.getSentBy().equals(sendTo)
                || e.getSentBy().equals(username)).collect(Collectors.toList());
        ObservableList<Message> messageObservableList = FXCollections.observableArrayList(tmpMessageList);
        this.chatContentList.setItems(messageObservableList);
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
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

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

    public List<String> getActives() {
        return actives;
    }

    public void setActives(List<String> actives) {
        this.actives = actives;
    }

    // public ObservableList<String> getChatArrayList() {
    //     return chatArrayList;
    // }
    //
    // public void setChatArrayList(ObservableList<String> chatArrayList) {
    //     this.chatArrayList = chatArrayList;
    // }

// public class ClientService implements Runnable {
    //     private final Socket socket;
    //
    //     private Controller controller;
    //     // private ReentrantLock searchActiveLock;
    //     private ObjectInputStream in;
    //     private ObjectOutputStream out;
    //     private Integer type;
    //
    //     private Boolean flagCheckLogin;
    //
    //     private Boolean flagSearchActives;
    //     private Boolean login;
    //     private String username;
    //     private List<String> actives;
    //     private List<Message> messageList;
    //     List<String> hasLinks;
    //     ObservableList<String> observableList;
    //
    //
    //     public ClientService(Socket socket, String username, ObjectInputStream in, ObjectOutputStream out, ReentrantLock searchActiveLock, Controller controller) {
    //         this.controller = controller;
    //         // this.searchActiveLock = searchActiveLock;
    //         this.socket = socket;
    //         this.username = username;
    //         this.in = in;
    //         this.out = out;
    //         this.actives = new ArrayList<>();
    //         this.login = false;
    //         this.flagCheckLogin = false;
    //         this.flagSearchActives = false;
    //         this.hasLinks = new ArrayList<>();
    //         this.observableList = FXCollections.observableArrayList(hasLinks);
    //         this.messageList = new ArrayList<>();
    //     }
    //
    //     @Override
    //     public void run() {
    //         try {
    //             try {
    //                 doClientService();
    //             } finally {
    //                 socket.close();
    //             }
    //         } catch (IOException | ClassNotFoundException e) {
    //             e.printStackTrace();
    //         }
    //     }
    //
    //     public void doClientService() throws IOException, ClassNotFoundException {
    //         while (true) {
    //             Object obj = in.readObject();
    //             if (obj == null) continue;
    //             Message message = (Message) obj;
    //             int type = message.getType();
    //             switch (type) {
    //                 case 1: {
    //                     System.out.println("client case 1");
    //                     whetherLogin(message);
    //                     this.flagCheckLogin = true;
    //                     break;
    //                 }
    //                 case 3: {
    //                     System.out.println("client case 3");
    //                     // 获取到了目前有多少人在线
    //                     getHowManyActives(message);
    //                     // List<String> list = message.getActives();
    //                     // Platform.runLater(()-> = list);
    //                     break;
    //                 }
    //                 case 5: {
    //                     System.out.println("client case 5");
    //                     saveMessage(message);
    //                     // addLinks(message);
    //                     break;
    //                 }
    //                 // case 7: {
    //                 //     System.out.println("client case 7");
    //                 //     handShaking2(message);
    //                 //     break;
    //                 // }
    //             }
    //         }
    //     }
    //
    //     public void whetherLogin(Message message) {
    //         if (message.getData().equals("success")) {
    //             this.login = true;
    //         } else {
    //             this.login = false;
    //             System.out.println("你的账号已在其他地方登录或密码错误");
    //         }
    //     }
    //
    //     public void getHowManyActives(Message message) {
    //         this.actives.add(message.getData());
    //         // this.controller.currentOnlineCnt.setText(String.valueOf(this.actives.size()));
    //         Platform.runLater(() -> {
    //             controller.setActives(this.actives);
    //             System.out.println("platform");
    //             // currentOnlineCnt.setText(String.valueOf(this.actives.size() + 1));
    //         });
    //     }
    //
    //     public void saveMessage(Message message) {
    //         this.messageList.add(message);
    //         ObservableList<Message> observableList = FXCollections.observableArrayList(this.messageList);
    //         Platform.runLater(() -> {
    //             controller.chatContentList.setItems(observableList);
    //         });
    //     }
    //
    //     public Boolean getFlagSearchActives() {
    //         return flagSearchActives;
    //     }
    //
    //     public void setFlagSearchActives(Boolean flagSearchActives) {
    //         this.flagSearchActives = flagSearchActives;
    //     }
    //
    //     public Integer getType() {
    //         return type;
    //     }
    //
    //     public void setType(Integer type) {
    //         this.type = type;
    //     }
    //
    //     public List<String> getActives() {
    //         return actives;
    //     }
    //
    //     public void setActives(List<String> actives) {
    //         this.actives = actives;
    //     }
    //
    //     public Boolean getLogin() {
    //         return login;
    //     }
    //
    //     public void setLogin(Boolean login) {
    //         this.login = login;
    //     }
    //
    //     public Boolean getFlagCheckLogin() {
    //         return flagCheckLogin;
    //     }
    //
    //     public void setFlagCheckLogin(Boolean flagCheckLogin) {
    //         this.flagCheckLogin = flagCheckLogin;
    //     }
    //
    // }

}

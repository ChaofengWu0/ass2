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
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println(socket);
                clientService = new ClientService(socket, username, in, out, this, sendTo);
                Thread thread = new Thread(clientService);
                thread.start();
                // handshaking1
                check();
                while (clientService.getFlagCheckLogin() == null) {
                    Thread.sleep(10);
                    // System.out.println(clientService.getFlagCheckLogin());
                }
                clientService.setFlagCheckLogin(null);
                // login没成功，走这里,要弹出警告
                try {
                    if (!clientService.getLogin()) {
                        throw new RuntimeException();
                    }
                } catch (RuntimeException e) {
                    thread.interrupt();
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("The user has already been online!\nPlease input a user not online!");
                    alert.showAndWait();
                    Platform.exit();
                    initialize(url, resourceBundle);
                    System.exit(0);
                    // return;
                }

                currentUsername.setText("Current User: " + username);
                // 成功上线，要发一个message
                loginSuccess();

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
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
                            this.clientService.setSelectedUsr(sendTo);
                            System.out.println("now sendTo: " + sendTo);
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
        if (selectedUser == null) return;
        if (clientService.observableList_chatList.contains(selectedUser)) {
            // 怎么打开这个对话框.
            System.out.println("已经有这个链接了");
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
    public synchronized void doSendMessage() throws IOException, InterruptedException {
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

        Long nowTime = System.currentTimeMillis();
        Message sendPrivateMessage = new Message(nowTime, username, sendTo, data, 4);
        this.clientService.messageList.add(sendPrivateMessage);
        System.out.println(username + " 现在有 " + this.clientService.messageList.size() + "条消息");
        out.writeObject(sendPrivateMessage);
        out.flush();
        // Thread.sleep(10);
        showMsg();
        this.chatContentList.setCellFactory(new MessageCellFactory());
        inputArea.clear();
    }


    public synchronized void showMsg() {
        // 下面的是处理信息的，并且显示在右上方
        List<Message> tmpMessageList = this.clientService.messageList.stream().filter(e ->
                e.getSentBy().equals(sendTo)
                        || (e.getSendTo().equals(sendTo))
        ).collect(Collectors.toList());

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
                        setText(null);
                        setGraphic(null);
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
}

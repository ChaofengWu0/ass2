package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

public class Controller implements Initializable {

    @FXML
    private Label currentUsername;

    @FXML
    private Label currentOnlineCnt;

    @FXML
    ListView<Message> chatContentList;


    private ReentrantLock lock;
    private ReentrantLock inLock;
    final int serverPort = 9999;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private String username;
    private ClientService clientService;

    public void check() throws IOException {
        // 发送给服务器，让服务器去查询当前username有没有在数据库中
        lock.lock();
        Message handshaking1 = new Message(0, this.username);
        out.writeObject(handshaking1);
        out.flush();
        lock.unlock();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // this.arrayList = new ArrayList<>();
        inLock = new ReentrantLock();
        lock = new ReentrantLock();
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
                clientService = new ClientService(socket, username, in, out, inLock);
                Thread thread = new Thread(clientService);
                thread.start();

                // handshaking1
                check();
                while (clientService.getFlagCheckLogin() == null) {

                }
//                 // login没成功，走这里,要弹出警告
                if (!clientService.getLogin()) {
// // 弹出警告
                }
                currentUsername.setText("Current User: " + username);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("username: " + username);
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }
        chatContentList.setCellFactory(new MessageCellFactory());
    }

    public void searchHowMany() throws IOException {
        lock.lock();
        Message howMany = new Message(2, "case2");
        out.writeObject(howMany);
        out.flush();
        lock.unlock();
        while (!clientService.getFlagSearchActives()) {
        }
        clientService.setFlagSearchActives(false);
    }

    // public void listhandShaking1(String username) throws IOException {
    //     lock.lock();
    //     Message linkSetUp = new Message(4, username);
    //     out.writeObject(linkSetUp);
    //     out.flush();
    //     lock.unlock();
    // }

    // public boolean linkSetUp(String username) throws IOException, InterruptedException {
    //     if (clientService.hasLinks.contains(username)) {
    //         System.out.println("已经和这个client建立过连接了，不需要再建立，源自linkSetUp这个方法");
    //         return false;
    //     } else {
    //         // 发送handshaking1的消息给server，server要处理这个消息，并且转发给client2
    //         listhandShaking1(username);
    //         Thread.sleep(200);
    //         System.out.println("link has set up");
    //         // System.out.println("before while in linkSetup");
    //         // while (!clientService.getFlagSetRequest()) {
    //         // }
    //         // clientService.setFlagSetRequest(false);
    //         // System.out.println("after while in linkSetup");
    //         // System.out.println("linkSetUp success");
    //         return true;
    //     }
    // }

    @FXML
    public void createPrivateChat() throws IOException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        searchHowMany();
        List<String> actives = clientService.getActives();
        // FIXME: get the user list from server, the current user's name should be filtered out
        for (String active : actives) {
            userSel.getItems().add(active);
        }
        this.currentOnlineCnt.setText(String.valueOf(actives.size()));

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
        if (clientService.hasLinks.contains(selectedUser)) {
            System.out.println("已经有这个链接了");
            // 打开就好
        } else {
            clientService.hasLinks.add(selectedUser);
            // 建立一个box
            clientService.hasLinks.add(selectedUser);
            // 建立一个box
            VBox chatItem = new VBox();
            Label titleLabel = new Label(selectedUser);
            // .getChildren().add(titleLabel);
            // chatItem.getStyleClass().add("chat-item");
            chatItem.setOnMouseClicked(event -> {
                // TODO: 点击打开聊天框
            });
            // leftPanel.getChildren().add(chatItem);

        }
        // listhandShaking1(selectedUser);
        // if (linkSetUp(selectedUser)) {
        // //     // System.out.println("after set up");
        // //     // Stage chatStage = new Stage();
        // //     // VBox chatBox = new VBox(10);
        // //     // chatBox.setPadding(new Insets(10));
        // //     // chatBox.setAlignment(Pos.TOP_LEFT);
        // //     //
        // //     // TextArea chatArea = new TextArea();
        // //     // chatArea.setEditable(false);
        // } else {
        // }


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
    public void doSendMessage() {
        // TODO
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


    public static class ClientService implements Runnable {
        private final Socket socket;

        private ReentrantLock inLock;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Integer type;

        private Boolean flagCheckLogin;

        private Boolean flagSearchActives;
        // private Boolean flagSetRequest;
        // private Boolean flagHandshakingReceive1;
        // private Boolean flagHandshakingReceive2;

        private Boolean login;
        private String username;
        private List<String> actives;
        private List<String> hasLinks;

        public ClientService(Socket socket, String username, ObjectInputStream in, ObjectOutputStream out, ReentrantLock inLock) {
            // ReentrantLock tmp = lock;
            this.inLock = inLock;


            this.socket = socket;
            this.username = username;
            this.in = in;
            this.out = out;
            this.actives = new ArrayList<>();
            this.login = false;
            this.flagCheckLogin = false;
            this.flagSearchActives = false;
            // this.flagSetRequest = false;
            // this.flagHandshakingReceive1 = false;
            // this.flagHandshakingReceive2 = false;
            this.hasLinks = new ArrayList<>();
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
                inLock.lock();
                System.out.println("before in");
                Object obj = in.readObject();
                System.out.println("after in");
                inLock.unlock();
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
            this.setActives(message.getActives());
            this.setFlagSearchActives(true);
        }

        public void addLinks(Message message) {
            this.hasLinks.add(message.getData());
        }

        // public void handShaking1(Message message) throws IOException {
        //     if (this.hasLinks.contains(message.getData())) {
        //         // 这个地方可以弹出窗口
        //         System.out.println("已经有链接了，不需要再建立，可以直接通信");
        //     } else {
        //         hasLinks.add(message.getData());
        //     }
        //     System.out.println("the username is this socket is : " + this.username +
        //             "and it has set up a link with " + message.getData());
        //     this.setFlagSetRequest(true);
        //     handShakingBack(message);
        // }
        //
        // public void handShakingBack(Message message) throws IOException {
        //     Message handShaking2 = new Message(4, message.getData());
        //     lock.lock();
        //     out.writeObject(handShaking2);
        //     out.flush();
        //     lock.unlock();
        // }

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

        // public Boolean getFlagSetRequest() {
        //     return flagSetRequest;
        // }
        //
        // public void setFlagSetRequest(Boolean flagSetRequest) {
        //     this.flagSetRequest = flagSetRequest;
        // }

        // public Boolean getFlagHandshakingReceive1() {
        //     return flagHandshakingReceive1;
        // }
        //
        // public void setFlagHandshakingReceive1(Boolean flagHandshakingReceive1) {
        //     this.flagHandshakingReceive1 = flagHandshakingReceive1;
        // }

        // public Boolean getFlagHandshakingReceive2() {
        //     return flagHandshakingReceive2;
        // }
        //
        // public void setFlagHandshakingReceive2(Boolean flagHandshakingReceive2) {
        //     this.flagHandshakingReceive2 = flagHandshakingReceive2;
        // }
    }


}

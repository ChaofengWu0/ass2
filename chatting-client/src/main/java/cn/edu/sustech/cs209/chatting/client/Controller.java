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
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    final int serverPort = 9999;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private String username;

    private ClientService clientService;
    // ArrayList<Socket> arrayList;

    public void check() throws IOException {
        // 发送给服务器，让服务器去查询当前username有没有在数据库中
        Message handshaking1 = new Message(0, this.username);
        out.writeObject(handshaking1);
        out.flush();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // this.arrayList = new ArrayList<>();

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
                socket = new Socket("localhost", 9999);
                // out = new ObjectOutputStream(socket.getOutputStream());
                // System.out.println("out");
                // in = new ObjectInputStream(socket.getInputStream());
                System.out.println("in");
                System.out.println(socket);
                clientService = new ClientService(socket, username);
                Thread thread = new Thread(clientService);
                thread.start();
                // while (!clientService.getLogin()) {
                // }
                System.out.println("after wait");

                // 发送给服务器，让服务器去查询当前username有没有在数据库中
                // Message handshaking1 = new Message(0, username);
                // out.writeObject(handshaking1);
                // out.flush();
                // while (true) {
                //     Object obj = in.readObject();
                //     if (obj == null) continue;
                //     Message message = (Message) obj;
                //     if (message.getType() == 1 && message.getData().equals("success")) {
                //         clientService = new ClientService(socket);
                //         Thread thread = new Thread(clientService);
                //         System.out.println("clientService runs...");
                //         thread.start();
                //     }
                //     break;
                // }
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

    @FXML
    public void createPrivateChat() {
        // clientService.doClientService();
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        List<String> actives = clientService.getActives();
        // FIXME: get the user list from server, the current user's name should be filtered out
        for (String active : actives) {
            userSel.getItems().addAll(active);
        }

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(200);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 30, 30, 30));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box,300,400));
        stage.showAndWait();

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
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Integer type;

        private Boolean login;
        private String username;
        private List<String> actives;

        //
        // public ClientService(Socket socket) {
        //     this.socket = socket;
        // }

        public ClientService(Socket socket, String username) {
            this.socket = socket;
            this.username = username;
            // String test = username;
            this.actives = new ArrayList<>();
            this.login = false;
        }

        @Override
        public void run() {
            try {
                try {
                    System.out.println("run...");
                    out = new ObjectOutputStream(this.socket.getOutputStream());
                    System.out.println("out");
                    in = new ObjectInputStream(this.socket.getInputStream());
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
            check();
            while (true) {
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
            System.out.println("check success");
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


}

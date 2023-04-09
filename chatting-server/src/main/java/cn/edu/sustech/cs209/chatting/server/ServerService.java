package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class ServerService implements Runnable {
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ConcurrentHashMap<String, Socket> hashMap;

    private Connection connection;

    public ServerService(Socket client, ConcurrentHashMap<String, Socket> hashMap) {
        this.client = client;
        this.hashMap = hashMap;
    }

    public Connection getConnection() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = null;
        try {
            String url = "jdbc:mysql://localhost:3306/java2?useSSL=FALSE&serverTimezone=UTC";
            String user = "root";
            String password = "123456";
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public void run() {
        try {
            // try {
            connection = getConnection();
            in = new ObjectInputStream(client.getInputStream());
            out = new ObjectOutputStream(client.getOutputStream());
            System.out.println("serverService run...");
            doService();
            // } finally {
            //     client.close();
            // }
        } catch (ClassNotFoundException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void doService() throws IOException, ClassNotFoundException, SQLException {
        while (true) {
            System.out.println("doService before in");
            Object obj = in.readObject();
            System.out.println("doService after in");
            if (obj == null) continue;
            Message message = (Message) obj;
            int type = message.getType();
            switch (type) {
                case 0: {
                    // 这里面还没有完善，要自己再弄弄
                    System.out.println("server case 0");
                    searchWhetherRunnable(message.getData());
                    break;
                }
                case 2: {
                    System.out.println("server case 2");
                    searchHowManyActive();
                    break;
                }
            }
        }
    }

    public void searchWhetherRunnable(String username) {
        try {
            PreparedStatement selectWhetherExist;
            PreparedStatement selectStatus;
            PreparedStatement allowLogin;

            String selectSQLExist = "select * from user where username = ?;";
            String selectSQLStatus = "select status from user where username = ? ";
            String allowLoginSQL = "update user set status = ? where username = ? ";

            selectWhetherExist = connection.prepareStatement(selectSQLExist);
            selectStatus = connection.prepareStatement(selectSQLStatus);
            allowLogin = connection.prepareStatement(allowLoginSQL);

            selectWhetherExist.setString(1, username);
            // if (selectWhetherExist.executeQuery().getFetchSize() != 0) {
            // 说明数据库里有这个用户，不能再创建

            // 先不查有没有这个用户
            // 我查他的登陆状态 规定1为登录 0为下线
            selectStatus.setString(1, username);
            ResultSet rs = selectStatus.executeQuery();
            rs.next();
            Integer status = rs.getInt(1);
            if (status != null && status == 0) {
                // 可以登录
                // 既然可以登录，那么记录此次登录，给hashmap填充
                hashMap.put(username, client);

                /**
                 * 暂时先注释
                 */
                // allowLogin.setInt(1, 1);
                // allowLogin.setString(2, username);
                // allowLogin.execute();

                // 发送信息给client
                Message loginSuccess = new Message(1, "success");
                out.writeObject(loginSuccess);
                out.flush();
                System.out.println("成功登录");
            } else {
                Message loginFailed = new Message(1, "failed");
                out.writeObject(loginFailed);
                out.flush();
                System.out.println("您的帐号在其他地方已经登陆");
            }
            // }
            // else {
            // 可以创建并且可以登录
            // }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void searchHowManyActive() throws SQLException, IOException {
        List<String> actives = new ArrayList<>();
        PreparedStatement searchHowMany;
        String searchHowManySQL = "select * from user where status is not null and status = ?";
        searchHowMany = connection.prepareStatement(searchHowManySQL);
        searchHowMany.setInt(1, 0);
        ResultSet rs = searchHowMany.executeQuery();
        while (rs.next()) {
            actives.add(rs.getString(1));
        }
        for (String active : actives) {
            System.out.println("actives : " + active);
        }
        Message howManyActiveResponse = new Message(3, actives);
        out.writeObject(howManyActiveResponse);
        out.flush();
    }

}
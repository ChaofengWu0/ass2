package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerService implements Runnable {
  private Socket client;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private ConcurrentHashMap<String, ServerService>
          hashMap;
  private Connection connection;
  private List<String> actives;

  public ServerService(Socket client, ConcurrentHashMap<String, ServerService> hashMap
          , List<String> actives) throws IOException {
    this.client = client;
    this.hashMap = hashMap;
    this.actives = actives;
    in = new ObjectInputStream(client.getInputStream());
    out = new ObjectOutputStream(client.getOutputStream());
  }

  // public Connection getConnection() throws ClassNotFoundException {
  //     Class.forName("com.mysql.cj.jdbc.Driver");
  //     Connection conn = null;
  //     try {
  //         String url = "jdbc:mysql://localhost:3306/java2?useSSL=FALSE&serverTimezone=UTC";
  //         String user = "root";
  //         String password = "123456";
  //         conn = DriverManager.getConnection(url, user, password);
  //     } catch (Exception e) {
  //         e.printStackTrace();
  //     }
  //     return conn;
  // }

  @Override
  public void run() {
    System.out.println("serverService run...");
    try {
      if (doService()) {
        Thread.currentThread().interrupt();
        return;
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      // throw new RuntimeException(e);
    }
  }

  public boolean doService() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
    boolean flag = false;
    while (true) {
      System.out.println("doService before in");
      Object obj = null;
      try {
        obj = in.readObject();
      } catch (IOException e) {
        System.out.println(client);
        e.printStackTrace();
      }
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
          tellAllClientsLogin(message);
          break;
        }
        case 4: {
          System.out.println("server case 4");
          establishLink1(message);
          break;
        }
        case 6: {
          System.out.println("server case 6");
          kill(message);
          flag = true;
          break;
        }
        case 8: {
          break;
        }
        case 10: {
          System.out.println("server case 10");
          file(message);
          break;
        }
      }
      if (flag) {
        Thread.sleep(10);
        return true;
      }
    }
  }

  public synchronized void searchWhetherRunnable(String username) throws IOException {
    if (!actives.contains(username)) {
      Message loginSuccess = new Message(1, "success");
      System.out.println("success");
      out.writeObject(loginSuccess);
      out.flush();
      actives.add(username);
      hashMap.put(username, this);
    } else {
      Message loginFailed = new Message(1, "failed");
      System.out.println("failed");
      out.writeObject(loginFailed);
      out.flush();
    }
    // try {
    // PreparedStatement selectWhetherExist;
    // PreparedStatement selectStatus;
    // PreparedStatement allowLogin;
    //
    // String selectSQLExist = "select * from user where username = ?;";
    // String selectSQLStatus = "select status from user where username = ? ";
    // String allowLoginSQL = "update user set status = ? where username = ? ";
    //
    // selectWhetherExist = connection.prepareStatement(selectSQLExist);
    // selectStatus = connection.prepareStatement(selectSQLStatus);
    // allowLogin = connection.prepareStatement(allowLoginSQL);
    //
    // selectWhetherExist.setString(1, username);
    /**
     * 从105到118是
     * 2023-4-12注释掉的数据库内容
     */

            /*
            // if (selectWhetherExist.executeQuery().getFetchSize() != 0) {
            // 说明数据库里有这个用户，不能再创建
            这一块是早于2023-4-12注释掉的内容
             */

    // 先不查有没有这个用户
    // 我查他的登陆状态 规定1为登录 0为下线
    // selectStatus.setString(1, username);
    // ResultSet rs = selectStatus.executeQuery();
    // rs.next();
    // Integer status = rs.getInt(1);
    //     if (status != null && status == 0) {
    //         // 可以登录
    //         // 既然可以登录，那么记录此次登录，给hashmap填充
    //         // lock.lock();
    //         hashMap.put(username, this);
    //         // lock.unlock();
    //         /**
    //          * 暂时先注释
    //          */
    //         // allowLogin.setInt(1, 1);
    //         // allowLogin.setString(2, username);
    //         // allowLogin.execute();
    //
    //         // 发送信息给client
    //         Message loginSuccess = new Message(1, "success");
    //         // outLock.lock();
    //         out.writeObject(loginSuccess);
    //         out.flush();
    //         // outLock.unlock();
    //         System.out.println("成功登录");
    //     } else {
    //         Message loginFailed = new Message(1, "failed");
    //         // outLock.lock();
    //         out.writeObject(loginFailed);
    //         out.flush();
    //         // outLock.unlock();
    //         System.out.println("您的帐号在其他地方已经登陆");
    //     }
    //     // }
    //     // else {
    //     // 可以创建并且可以登录
    //     // }
    // } catch (SQLException | IOException e) {
    //     throw new RuntimeException(e);
    // }
  }

  public synchronized void tellAllClientsLogin(Message message) {
    String userName = message.getData();
    // 和所有人说，userName上线了
    hashMap.forEach((key, value) -> {
      if (!key.equals(userName)) {
        System.out.println(key);
        Message notification = new Message(3, userName);
        try {
          value.out.writeObject(notification);
          value.out.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    // 告诉userName，哪些人在线
    hashMap.forEach((key, value) -> {
      if (!key.equals(userName)) {
        System.out.println(key);
        Message notification = new Message(3, key);
        try {
          hashMap.get(userName).out.writeObject(notification);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
    // lock.unlock();
  }

  public synchronized void establishLink1(Message message) {
    String sendToUsername = message.getSendTo();
    // 处理多个人的情况
    String[] sendToUsrArr = sendToUsername.split(",");
    if (sendToUsrArr.length > 1) {
      for (int i = 0; i < sendToUsrArr.length; i++) {
        sendToUsrArr[i] = sendToUsrArr[i].trim();
      }
    } else {
      if (!this.actives.contains(sendToUsername)) return;
      sendMsgPrivate(message, sendToUsername);
      return;
    }

    // 给每个人都发一遍消息
    // boolean groupFlag = false;
    for (String s : sendToUsrArr) {
      if (!this.actives.contains(s)) {
        continue;
      }
      // groupFlag = true;
      if (s.equals(message.getSentBy())) continue;
      sendToUsername = s;
      ServerService sendTo = hashMap.get(sendToUsername);
      System.out.println(sendTo);
      try {
        message.setType(5);
        sendTo.out.writeObject(message);
        sendTo.out.flush();
        System.out.println("Server:\nReceive from " + client.getPort() + " and send to " + sendToUsername);
        System.out.println("The msg is " + message.getData());
        System.out.println("The msg if for group " + message.getSendTo());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void sendMsgPrivate(Message message, String sendToUsername) {
    ServerService sendTo = hashMap.get(sendToUsername);
    System.out.println(sendTo);
    try {
      message.setType(5);
      sendTo.out.writeObject(message);
      sendTo.out.flush();
      System.out.println("Server:\nReceive from " + client.getPort() + " and send to " + sendToUsername);
      System.out.println("The msg is " + message.getData());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendMsgPrivateFile(Message message, String sendToUsername) {
    ServerService sendTo = hashMap.get(sendToUsername);
    System.out.println(sendTo);
    try {
      message.setType(11);
      sendTo.out.writeObject(message);
      sendTo.out.flush();
      System.out.println("Server:\nReceive from " + client.getPort() + " and send to " + sendToUsername);
      System.out.println("The msg is " + message.getData());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void kill(Message message) throws IOException {
    String deadUsr = message.getData();
    System.out.println("The usr will be dead " + deadUsr);
    // server为其分配的内容要删掉，然后告诉所有人这个人死掉了
    String dead = null;
    // List<String> tmp = new ArrayList<>(this.actives);
    for (String active : actives) {
      if (active.equals(deadUsr)) {
        dead = active;
        closeAll(active);
        continue;
      }
      Message deadNotification = new Message(7, deadUsr);
      hashMap.get(active).out.writeObject(deadNotification);
      hashMap.get(active).out.flush();
    }
    if (dead != null) {
      this.actives.remove(dead);
    }
  }

  public synchronized void file(Message message) {
    String sendToUsername = message.getSendTo();
    // 处理多个人的情况
    String[] sendToUsrArr = sendToUsername.split(",");
    if (sendToUsrArr.length > 1) {
      for (int i = 0; i < sendToUsrArr.length; i++) {
        sendToUsrArr[i] = sendToUsrArr[i].trim();
      }
    } else {
      if (!this.actives.contains(sendToUsername)) return;
      sendMsgPrivateFile(message, sendToUsername);
      return;
    }

    // 给每个人都发一遍消息
    // boolean groupFlag = false;
    for (String s : sendToUsrArr) {
      if (!this.actives.contains(s)) {
        continue;
      }
      // groupFlag = true;
      if (s.equals(message.getSentBy())) continue;
      sendToUsername = s;
      ServerService sendTo = hashMap.get(sendToUsername);
      System.out.println(sendTo);
      try {
        message.setType(11);
        sendTo.out.writeObject(message);
        sendTo.out.flush();
        System.out.println("Server:\nReceive from " + client.getPort() + " and send to " + sendToUsername);
        System.out.println("The msg is " + message.getData());
        System.out.println("The msg if for group " + message.getSendTo());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  public void closeAll(String active) throws IOException {
    hashMap.get(active).in.close();
    hashMap.get(active).out.close();
    hashMap.get(active).client.close();
    hashMap.remove(active);
  }


}
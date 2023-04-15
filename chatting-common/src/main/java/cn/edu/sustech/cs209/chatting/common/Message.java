package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long timestamp;

  private String sentBy;

  private String sendTo;

  // private List<String> actives = new ArrayList<>();
  private String data;

  // 0为client向server询问是否可以登录
  // 1为server回给client是否可以登录

  // 2为client向server告知自己上线了
  // 3为server向其余所有client告知有一个client上线了
  // 4为client1向client2发送的消息，需要经过server转发
  // 5为server转发给client2的type
  // 6为client向server发送他死了的消息
  // 7为server向各个其他client发送他死了的消息
  // 8为client向server发送心跳检测包
  private int type;


  public Message(int type, String data) {
    this.type = type;
    this.data = data;
  }


  public Message(String sentBy, String sendTo, int type) {
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.type = type;
  }

  public Message(Long timestamp, String sentBy, String sendTo, String data, int type) {
    this.timestamp = timestamp;
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.data = data;
    this.type = type;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public void setSentBy(String sentBy) {
    this.sentBy = sentBy;
  }

  public void setSendTo(String sendTo) {
    this.sendTo = sendTo;
  }

  public void setData(String data) {
    this.data = data;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getSendTo() {
    return sendTo;
  }

  public String getData() {
    return data;
  }

  // public List<String> getActives() {
  //     return actives;
  // }
  //
  // public void setActives(List<String> actives) {
  //     this.actives = actives;
  // }
}

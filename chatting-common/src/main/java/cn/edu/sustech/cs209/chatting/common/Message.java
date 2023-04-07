package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private List<String> actives = new ArrayList<>();
    private String data;

    // 0为client向server询问是否可以登录
    // 1为server回给client是否可以登录
    // 2为client向server询问当前在线的用户分别是谁。
    // 3为server向client告知现在在线的有多少,以list形式返回，list里面装的是client的username。
    private int type;


    public Message(int type, String data) {
        this.type = type;
        this.data = data;
    }

    public Message(int type, List<String> actives) {
        this.actives = actives;
        this.type = type;
    }

    public Message(Long timestamp, String sentBy, String sendTo, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
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

    public List<String> getActives() {
        return actives;
    }

    public void setActives(List<String> actives) {
        this.actives = actives;
    }
}

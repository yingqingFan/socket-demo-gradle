package com.ls.socket.entity;

import java.io.Serializable;
import java.util.Date;

public class MessageInfo implements Serializable {
    private static final long serialVersionUID = 3116836935624567198L;
    private String messageId;
    private String action;
    private String clientId;
    private String friendClientId;
    private String messageContent;
    private Date date;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getFriendClientId() {
        return friendClientId;
    }

    public void setFriendClientId(String friendClientId) {
        this.friendClientId = friendClientId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}

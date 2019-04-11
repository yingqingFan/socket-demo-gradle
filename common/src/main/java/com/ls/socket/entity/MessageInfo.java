package com.ls.socket.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class MessageInfo implements Serializable {
    private static final long serialVersionUID = 3116836935624567198L;
    private String messageId;
    //操作类型
    private String action;
    //当前用户
    private String userId;
    //聊天室
    private String roomId;
    //消息内容
    private String messageContent;
    //消息时间
    private Date date;
    //消息浏览标记
    private String messageMarkId;
    //对方的用户名（主要用来检查用户是否存在和查询与用户间历史记录）
    private String checkUserId;
    //创建多人聊天的用户名
    private String[] userIds;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    public String getMessageMarkId() {
        return messageMarkId;
    }

    public void setMessageMarkId(String messageMarkId) {
        this.messageMarkId = messageMarkId;
    }

    public String getCheckUserId() {
        return checkUserId;
    }

    public void setCheckUserId(String checkUserId) {
        this.checkUserId = checkUserId;
    }

    public String[] getUserIds() {
        return userIds;
    }

    public void setUserIds(String[] userIds) {
        this.userIds = userIds;
    }
}

package com.ls.socket.entity;

import java.util.Date;

public class ChatRoom {
    public static String CHAT_TYPE_SINGLE = "S";
    public static String CHAT_TYPE_GROUP = "G";
    private String roomId;
    private String roomType;
    private Date createDate;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}

package com.ls.socket.entity;

public class ChatRoom {
    public static String CHAT_TYPE_SINGLE = "S";
    public static String CHAT_TYPE_GROUP = "G";
    private String roomId;
    private String roomType;

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
}

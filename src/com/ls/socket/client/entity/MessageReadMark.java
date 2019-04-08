package com.ls.socket.client.entity;

/**
 * 标记信息读取位置，每个客户端对应一个
 */
public class MessageReadMark {
    /**
     * 当前读取到的messageId
     */
    private String messageId;

    /**
     * roomId: 聊天室
     */
    private String roomId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}

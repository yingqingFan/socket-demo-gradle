package com.ls.socket.entity;

import java.util.Date;

public class RoomUser {
    private String roomUserId;
    private String roomId;
    private String userId;
    private Date createDate;

    public String getRoomUserId() {
        return roomUserId;
    }

    public void setRoomUserId(String roomUserId) {
        this.roomUserId = roomUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}

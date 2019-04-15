package com.ls.socket.service;

import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.RoomUser;
import com.ls.socket.util.DataUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RoomUserService {
    public static String ROOM_USER_FILE_PATH = null;
    public List<String> getUserIdsByRoomId(String roomId){
        List<String> userIds = new ArrayList<String>();
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers.size() > 0){
            for(int i = 0; i < roomUsers.size(); i++){
                if(roomUsers.get(i).getRoomId().equals(roomId)){
                    userIds.add(roomUsers.get(i).getUserId());
                }
            }
        }
        return userIds;
    }

    public List<String> getChatRomIdsByUserIdAndRoomType(String userId, String roomType){
        List<String> roomIds = new ArrayList<String>();
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers != null && roomUsers.size() > 0){
            RoomService roomService = new RoomService();
            for(int i = 0; i < roomUsers.size(); i++){
                if(roomUsers.get(i) != null) {
                    if(!StringUtils.isEmpty(roomUsers.get(i).getUserId())) {
                        String roomId = roomUsers.get(i).getRoomId();
                        ChatRoom room = roomService.getRoomByRoomId(roomId);
                        if(room.getRoomType().equals(roomType)) {
                            if (roomUsers.get(i).getUserId().equals(userId)) {
                                roomIds.add(roomId);
                            }
                        }
                    }
                }
            }
        }
        return roomIds;
    }

    public List<ChatRoom> getChatRomsByUserId(String userId){
        List<ChatRoom> rooms = new ArrayList<ChatRoom>();
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers != null && roomUsers.size() > 0){
            RoomService roomService = new RoomService();
            for(int i = 0; i < roomUsers.size(); i++){
                if(roomUsers.get(i) != null) {
                    if (!StringUtils.isEmpty(roomUsers.get(i).getUserId())) {
                        String roomId = roomUsers.get(i).getRoomId();
                        ChatRoom room = roomService.getRoomByRoomId(roomId);
                        if (roomUsers.get(i).getUserId().equals(userId)) {
                            rooms.add(room);
                        }
                    }
                }
            }
        }
        return rooms;
    }

    public String getSingleRoomIdByUserIds(String id1, String id2){
        String roomId = null;
        //获取共同的roomId
        List<String> roomIds1 = getChatRomIdsByUserIdAndRoomType(id1,ChatRoom.CHAT_TYPE_SINGLE);
        List<String> roomIds2 = getChatRomIdsByUserIdAndRoomType(id2,ChatRoom.CHAT_TYPE_SINGLE);
        List<String> exists = new ArrayList<String>(roomIds1);
        List<String> notexists = new ArrayList<String>(roomIds1);
        //roomIds1去除相同的值,exists结果为不同的值
        exists.removeAll(roomIds2);
        //roomIds1去除不相同的值，notexists结果为相同值
        notexists.removeAll(exists);
        if(notexists.size()>0) {
            roomId = notexists.get(0);
        }else{
            roomId = null;
        }
        return roomId;
    }

    public synchronized RoomUser saveRoomUser(RoomUser roomUser){
        String userId = roomUser.getUserId();
        String roomId = roomUser.getRoomId();
        if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(roomId)){
            return null;
        }
        RoomUser roomUser1 = getRoomUserByRoomIdAndUserId(roomId, userId);
        if(roomUser1!=null){
            return roomUser1;
        }
        int id = 1;
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers.size()>0){
            if(roomUsers.get(roomUsers.size()-1) != null) {
                String latestIdStr = roomUsers.get(roomUsers.size() - 1).getRoomUserId();
                int latestId = Integer.parseInt(latestIdStr);
                id = latestId + 1;
            }
        }
        roomUser.setRoomUserId(id+"");
        roomUser.setCreateDate(new Date());
        new DataUtil<RoomUser>().writeToFile(ROOM_USER_FILE_PATH, roomUser);
        return roomUser;
    }

    public RoomUser getRoomUserByRoomIdAndUserId(String roomId, String userId){
        RoomUser result = null;
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers.size()>0){
            for (int i = 0; i < roomUsers.size(); i++) {
                RoomUser roomUser = roomUsers.get(i);
                if(roomUser.getRoomId().equals(roomId) && roomUser.getUserId().equals(userId)){
                    result = roomUser;
                    return result;
                }
            }
        }
        return result;
    }

    public void deleteRoomUser(String roomId, String userId){
        DataUtil<RoomUser> dataUtil = new DataUtil<RoomUser>();
        List<RoomUser> roomUsers = dataUtil.readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        dataUtil.clearFile(ROOM_USER_FILE_PATH);
        if(roomUsers.size()>0){
            for (int i = 0; i < roomUsers.size(); i++) {
                RoomUser roomUser = roomUsers.get(i);
                if(!(roomUser.getRoomId().equals(roomId) && roomUser.getUserId().equals(userId))){
                    dataUtil.writeToFile(ROOM_USER_FILE_PATH, roomUser);
                }
            }
        }
    }

}

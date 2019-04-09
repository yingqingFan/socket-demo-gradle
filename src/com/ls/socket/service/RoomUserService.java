package com.ls.socket.service;

import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.RoomUser;
import com.ls.socket.util.DataUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RoomUserService {
    public static String ROOM_FILE_PATH = null;
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

    public List<String> getChatRomIdsByUserId(String userId){
        List<String> roomIds = new ArrayList<String>();
        List<RoomUser> roomUsers = new DataUtil<RoomUser>().readFromFile(ROOM_USER_FILE_PATH,RoomUser.class);
        if(roomUsers != null && roomUsers.size() > 0){
            for(int i = 0; i < roomUsers.size(); i++){
                if(roomUsers.get(i) != null) {
                    if(!StringUtils.isEmpty(roomUsers.get(i).getUserId())) {
                        if (roomUsers.get(i).getUserId().equals(userId)) {
                            roomIds.add(roomUsers.get(i).getRoomId());
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
            for(int i = 0; i < roomUsers.size(); i++){
                if(roomUsers.get(i) != null) {
                    if (!StringUtils.isEmpty(roomUsers.get(i).getUserId())) {
                        if (roomUsers.get(i).getUserId().equals(userId)) {
                            String roomId = roomUsers.get(i).getRoomId();
                            ChatRoom room = getRoomByRoomId(roomId);
                            rooms.add(room);
                        }
                    }
                }
            }
        }
        return rooms;
    }

    public ChatRoom getRoomByRoomId(String roomId){
        ChatRoom result = null;
        List<ChatRoom> rooms = new DataUtil<ChatRoom>().readFromFile(ROOM_FILE_PATH,ChatRoom.class);
        for (int i = 0; i < rooms.size(); i++) {
            ChatRoom room  = rooms.get(i);
            if(room.getRoomId().equals(roomId)){
                result = room;
            }
        }
        return result;
    }

    public String getSingleRoomIdByUserIds(String id1, String id2){
        String roomId = null;
        //获取共同的roomId
        List<String> roomIds1 = getChatRomIdsByUserId(id1);
        List<String> roomIds2 = getChatRomIdsByUserId(id2);
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

    //新建单聊room并绑定用户
    public ChatRoom createSingleChatRoom(String userId1, String userId2){
        ChatRoom room = new ChatRoom();
        room.setRoomType(ChatRoom.CHAT_TYPE_SINGLE);
        RoomUser roomUser1 = new RoomUser();
        roomUser1.setUserId(userId1);
        RoomUser roomUser2 = new RoomUser();
        roomUser2.setUserId(userId2);
        //保存room到文件
        room = saveRoom(room);
        //保存roomUser到文件
        roomUser1.setRoomId(room.getRoomId());
        roomUser2.setRoomId(room.getRoomId());
        saveRoomUser(roomUser1);
        saveRoomUser(roomUser2);
        return room;
    }

    public synchronized ChatRoom saveRoom(ChatRoom room){
        int id = 1;
        List<ChatRoom> chatRooms = new DataUtil<ChatRoom>().readFromFile(ROOM_FILE_PATH,ChatRoom.class);
        if(chatRooms.size()>0){
            String latestIdStr = chatRooms.get(chatRooms.size()-1).getRoomId();
            int latestId = Integer.parseInt(latestIdStr);
            id = latestId+1;
        }
        room.setRoomId(id+"");
        new DataUtil<ChatRoom>().writeToFile(ROOM_FILE_PATH, room);
        return room;
    }

    public synchronized RoomUser saveRoomUser(RoomUser roomUser){
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
        new DataUtil<RoomUser>().writeToFile(ROOM_USER_FILE_PATH, roomUser);
        return roomUser;
    }

}

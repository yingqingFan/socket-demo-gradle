package com.ls.socket.service;

import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.RoomUser;
import com.ls.socket.util.DataUtil;

import java.util.Date;
import java.util.List;

public class RoomService {
    public static String ROOM_FILE_PATH = null;
    public synchronized ChatRoom saveRoom(ChatRoom room){
        int id = 1;
        List<ChatRoom> chatRooms = new DataUtil<ChatRoom>().readFromFile(ROOM_FILE_PATH,ChatRoom.class);
        if(chatRooms.size()>0){
            String latestIdStr = chatRooms.get(chatRooms.size()-1).getRoomId();
            int latestId = Integer.parseInt(latestIdStr);
            id = latestId+1;
        }
        room.setRoomId(id+"");
        room.setCreateDate(new Date());
        new DataUtil<ChatRoom>().writeToFile(ROOM_FILE_PATH, room);
        return room;
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
        RoomUserService roomUserService = new RoomUserService();
        roomUserService.saveRoomUser(roomUser1);
        roomUserService.saveRoomUser(roomUser2);
        return room;
    }

    public void deleteRoom(String roomId){
        DataUtil<ChatRoom> dataUtil = new DataUtil<ChatRoom>();
        List<ChatRoom> rooms = dataUtil.readFromFile(ROOM_FILE_PATH,ChatRoom.class);
        dataUtil.clearFile(ROOM_FILE_PATH);
        if(rooms.size()>0){
            for (int i = 0; i < rooms.size(); i++) {
                ChatRoom room = rooms.get(i);
                if(!(room.getRoomId().equals(roomId))){
                    dataUtil.writeToFile(ROOM_FILE_PATH, room);
                }
            }
        }
    }
}

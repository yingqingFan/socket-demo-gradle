package com.ls.socket.service;

import com.ls.socket.entity.MessageReadMark;
import com.ls.socket.util.DataUtil;

import java.util.List;

public class MessageReadMarkService {
    public static String MESSAGE_READ_MARK_FILE_PATH = null;

    public MessageReadMark getMessageReadMarkByRoomId(String roomId){
        MessageReadMark result = null;
        List<MessageReadMark> messageReadMarks = new DataUtil<MessageReadMark>().readFromFile(MESSAGE_READ_MARK_FILE_PATH, MessageReadMark.class);
        if (messageReadMarks.size()>0){
            for (int i = 0; i < messageReadMarks.size(); i++) {
                MessageReadMark messageReadMark = messageReadMarks.get(i);
                if(messageReadMark.getRoomId().equals(roomId)){
                    result = messageReadMark;
                }
            }
        }
        return result;
    }

    public void saveMessageReadMark(MessageReadMark messageReadMark){
        new DataUtil<MessageReadMark>().writeToFile(MESSAGE_READ_MARK_FILE_PATH, messageReadMark);
    }
}

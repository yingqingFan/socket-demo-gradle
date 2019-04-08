package com.ls.socket.service;

import com.ls.socket.entity.MessageInfo;
import com.ls.socket.server.SocketServer;
import com.ls.socket.util.DataUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageInfoService {

    public static String MESSAGE_FILE_PATH = null;

    public List<MessageInfo> getMessageInfosByRoomId(String roomId){
        List<MessageInfo> messageInfos = new ArrayList<MessageInfo>();
        List<MessageInfo> allMessageInfos = new DataUtil<MessageInfo>().readFromFile(MESSAGE_FILE_PATH, MessageInfo.class);
        if(allMessageInfos.size()>0) {
            for (int i = 0; i < allMessageInfos.size(); i++) {
                if (allMessageInfos.get(i).getRoomId().equals(roomId)) {
                    messageInfos.add(allMessageInfos.get(i));
                }
            }
        }
        return messageInfos;
    }

    public synchronized MessageInfo saveMessageInfo(MessageInfo messageInfo){
        int id = 1;
        List<MessageInfo> messageInfos = new DataUtil<MessageInfo>().readFromFile(MESSAGE_FILE_PATH,MessageInfo.class);
        if(messageInfos.size()>0){
            String latestIdStr = messageInfos.get(messageInfos.size()-1).getMessageId();
            int latestId = Integer.parseInt(latestIdStr);
            id = latestId+1;
        }
        messageInfo.setMessageId(id+"");
        new DataUtil<MessageInfo>().writeToFile(MESSAGE_FILE_PATH,messageInfo);
        return messageInfo;
    }

}

package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.client.entity.MessageReadMark;
import com.ls.socket.client.service.MessageReadMarkService;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.service.RoomUserService;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class ReceiveThread extends Thread{
    private static Logger log = Logger.getLogger(ReceiveThread.class);
    private Socket socket;
    private BufferedReader bufferedReader;
    public ReceiveThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String line = null;
                while (((line = bufferedReader.readLine()) != null)) {
                    MessageInfo messageInfo = new Gson().fromJson(line, MessageInfo.class);
                    if (messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[0])) {
                        if (StringUtils.isEmpty(SocketClient.ROOM_ID)) {
                            System.out.println(messageInfo.getMessageContent());
                        } else {
                            RoomUserService roomUserService = new RoomUserService();
                            List<String> userIds = roomUserService.getUserIdsByRoomId(SocketClient.ROOM_ID);
                            if(userIds != null && userIds.size()>0){
                                if(userIds.contains(messageInfo.getClientId())){
                                    System.out.println(messageInfo.getMessageContent());
                                    String messageId = messageInfo.getMessageId();
                                    MessageReadMark messageReadMark = new MessageReadMark();
                                    messageReadMark.setMessageId(messageId);
                                    messageReadMark.setRoomId(SocketClient.ROOM_ID);
                                    new MessageReadMarkService().saveMessageReadMark(messageReadMark);
                                }
                            }
                        }
                    } else if (messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[6])){
                        log.error(messageInfo.getMessageContent());
                        System.exit(0);
                    } else {
                        System.out.println(messageInfo.getMessageContent());
                    }
                }
            }
        } catch (IOException e) {
            log.error("error",e);
            try {
                socket.close();
            } catch (IOException e1) {
                log.error("error",e1);
            }
            if(bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    log.error("error",e1);
                }
            }
            log.debug("无法连接服务器");
            return;
        }
    }
}

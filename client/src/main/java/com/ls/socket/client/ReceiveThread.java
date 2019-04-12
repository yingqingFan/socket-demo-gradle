package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.entity.MessageReadMark;
import com.ls.socket.service.MessageReadMarkService;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

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
                            if(messageInfo.getRoomId().equals(SocketClient.ROOM_ID)){
                                System.out.println(messageInfo.getMessageContent());
                                String messageId = messageInfo.getMessageId();
                                MessageReadMark messageReadMark = new MessageReadMark();
                                messageReadMark.setMessageId(messageId);
                                messageReadMark.setRoomId(SocketClient.ROOM_ID);
                                new MessageReadMarkService().saveMessageReadMark(messageReadMark);
                            }
                        }
                    }else if (messageInfo.getAction() != null && (messageInfo.getAction().equals(SocketUtil.ACTIONS[1]) || messageInfo.getAction().equals(SocketUtil.ACTIONS[13]))){
                        SocketClient.ACTION = null;
                        System.out.println(messageInfo.getMessageContent());
                        saveMessageReadMarkByMessageInfo(messageInfo);
                        SocketClient.IS_RESPONSE = "true";
                    }else if (messageInfo.getAction().equals(SocketUtil.ACTIONS[2])){
                        SocketClient.ACTION = null;
                        System.out.println(messageInfo.getMessageContent());
                        SocketClient.CHOOSE_NO = "0";
                        SocketClient.IS_RESPONSE = "true";
                    }else if(messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[5])){
                        if(!StringUtils.isEmpty(messageInfo.getMessageContent())) {
                            System.out.println(messageInfo.getMessageContent());
                        }
                        String roomId = messageInfo.getRoomId();
                        if(StringUtils.isEmpty(roomId)){
                            SocketClient.USER_CHECK = "false";
                        }else{
                            SocketClient.USER_CHECK = "true";
                            SocketClient.ROOM_ID = roomId;
                        }
                        SocketClient.IS_RESPONSE = "true";
                    }else if (messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[6])){
                        log.error(messageInfo.getMessageContent());
                        System.exit(0);
                    }else if(messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[7])){
                        System.out.println(messageInfo.getMessageContent());
                        saveMessageReadMarkByMessageInfo(messageInfo);
                        SocketClient.IS_RESPONSE = "true";
                    }else if(messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[8])){
                        System.out.println(messageInfo.getMessageContent());
                        SocketClient.IS_RESPONSE = "true";
                    }else if (messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[9])){
                        SocketClient.ACTION = null;
                        System.out.println(messageInfo.getMessageContent());
                        SocketClient.CHOOSE_NO = "5";
                        SocketClient.IS_RESPONSE = "true";
                    }else if(messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[11])){
                        if(!StringUtils.isEmpty(messageInfo.getMessageContent())) {
                            System.out.println(messageInfo.getMessageContent());
                        }
                        String roomId = messageInfo.getRoomId();
                        if(StringUtils.isEmpty(roomId)){
                            SocketClient.ROOM_CHECK = "false";
                        }else{
                            SocketClient.ROOM_CHECK = "true";
                            SocketClient.ROOM_ID = roomId;
                        }
                        SocketClient.IS_RESPONSE = "true";
                    }else if(messageInfo.getAction() != null && messageInfo.getAction().equals(SocketUtil.ACTIONS[12])){
                        System.out.println(messageInfo.getMessageContent());
                        SocketClient.IS_RESPONSE = "true";
                    } else {
                        System.out.println(messageInfo.getMessageContent());
                    }
                }
            }
        } catch (IOException e) {
            log.error("IOException",e);
            try {
                socket.close();
            } catch (IOException e1) {
                log.error("IOException",e1);
            }
            if(bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    log.error("IOException",e1);
                }
            }
            log.debug("无法连接服务器");
            return;
        }
    }

    public void saveMessageReadMarkByMessageInfo(MessageInfo messageInfo){
        String messageMarkId = messageInfo.getMessageMarkId();
        MessageReadMark messageReadMark = new MessageReadMark();
        messageReadMark.setRoomId(messageInfo.getRoomId());
        messageReadMark.setMessageId(messageMarkId);
        new MessageReadMarkService().saveMessageReadMark(messageReadMark);
    }
}

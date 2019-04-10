package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.entity.MessageReadMark;
import com.ls.socket.service.MessageReadMarkService;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class SendThread extends Thread{
    private static Logger log = Logger.getLogger(SendThread.class);
    private Socket socket;
    private PrintWriter writer;
    public SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream());
            SocketClient.IS_RESPONSE = "Y";
            while (true) {
                MessageInfo messageInfo = initMessageInfo();
                if (messageInfo == null) {
                    continue;
                }
                messageInfo.setDate(new Date());
                String str = new Gson().toJson(messageInfo);
                //发送数据到服务端
                writer.println(str);
                writer.flush();
            }
        }catch (IOException e) {
            log.error("IOException",e);
            try {
                socket.close();
            } catch (IOException e1) {
                log.error("IOException",e1);
            }
            if(writer != null) {
               writer.close();
            }
            return;
        }
    }

    public MessageInfo initMessageInfo(){
        if(SocketClient.ACTION == null || !SocketClient.ACTION.equals(SocketUtil.ACTIONS[0])){
            waitResponse();
        }
        MessageInfo messageInfo = null;
        Scanner scanner = new Scanner(System.in);
        if(SocketClient.ACTION != null){
            if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[7])){
                messageInfo = showUnReadRoomHistory();
            } else if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[0])){
                messageInfo = completeSendMessageInfoByRoomId(SocketClient.ROOM_ID);
            }else if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[1])){
                messageInfo = showUserHistory();
            }
        }else {
            System.out.println("选择序号：0." + SocketUtil.ACTIONS[0] + " 1." + SocketUtil.ACTIONS[1] + " 2." + SocketUtil.ACTIONS[2]);
            String orderNumber = scanner.next();
            switch (orderNumber) {
                case "0":
                    SocketClient.ACTION = SocketUtil.ACTIONS[7];
                    System.out.println("请输入对方用户名(按Enter键发送消息,按#键加Enter退出聊天):");
                    String userIdTo = scanner.next();
                    //check user
                    messageInfo = checkUser(userIdTo);
                    break;
                case "1":
                    SocketClient.ACTION = SocketUtil.ACTIONS[1];
                    System.out.println("请输入对方用户名：");
                    String userId = scanner.next();
                    //check user
                    messageInfo = checkUser(userId);
                    break;
                case "2":
                    messageInfo = new MessageInfo();
                    messageInfo.setAction(SocketUtil.ACTIONS[2]);
                    SocketClient.ACTION = SocketUtil.ACTIONS[2];
                    break;
                default:
                    System.out.println("没有该选项，请重新选择!");
                    SocketClient.IS_RESPONSE = "true";
                    break;
            }
        }
        return messageInfo;
    }

    public MessageInfo checkUser(String userId){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[5]);
        messageInfo.setCheckUserId(userId);
        messageInfo.setUserId(SocketClient.USER_ID);
        return messageInfo;
    }

    public MessageInfo showUnReadRoomHistory(){
        if(SocketClient.USER_EXIST.equals("true")){
            System.out.println("提示：已进入聊天室（光标处输入想要发送的消息，按Enter键发送）");
            MessageReadMarkService messageReadMarkService = new MessageReadMarkService();
            MessageReadMark messageReadMark = messageReadMarkService.getMessageReadMarkByRoomId(SocketClient.ROOM_ID);
            if (messageReadMark == null) {
                messageReadMark = new MessageReadMark();
                String messageMarkId = "0";
                messageReadMark.setRoomId(SocketClient.ROOM_ID);
                messageReadMark.setMessageId(messageMarkId);
                messageReadMarkService.saveMessageReadMark(messageReadMark);
            }
            //获取记录读取标记
            String messageMarkId = messageReadMark.getMessageId();
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setAction(SocketUtil.ACTIONS[7]);
            messageInfo.setRoomId(SocketClient.ROOM_ID);
            messageInfo.setMessageMarkId(messageMarkId);
            messageInfo.setUserId(SocketClient.USER_ID);
            SocketClient.ACTION = SocketUtil.ACTIONS[0];
            SocketClient.USER_EXIST = null;
            return messageInfo;
        }else{
            System.out.println("用户不存在");
            SocketClient.USER_EXIST = null;
            SocketClient.ACTION = null;
            SocketClient.IS_RESPONSE = "true";
            return null;
        }
    }

    public MessageInfo showUserHistory(){
        if(SocketClient.USER_EXIST.equals("true")) {
            MessageInfo messageInfo =completeViewHistoryMessageInfoByRoomId(SocketClient.ROOM_ID);
            SocketClient.USER_EXIST = null;
            SocketClient.ACTION = null;
            return messageInfo;
        }else{
            System.out.println("用户不存在");
            SocketClient.USER_EXIST = null;
            SocketClient.ACTION = null;
            SocketClient.IS_RESPONSE = "true";
            return null;
        }
    }

    public MessageInfo completeSendMessageInfoByRoomId(String roomId){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setRoomId(roomId);
        messageInfo.setAction(SocketClient.ACTION);
        messageInfo.setUserId(SocketClient.USER_ID);
        Scanner scanner = new Scanner(System.in);
        String messageContent = scanner.next();
        if (messageContent.equals("#")) {
            SocketClient.ACTION = null;
            SocketClient.ROOM_ID = null;
            SocketClient.IS_RESPONSE = "true";
            return null;
        }
        messageInfo.setMessageContent(messageContent);
        return messageInfo;
    }

    public MessageInfo completeViewHistoryMessageInfoByRoomId(String roomId){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setRoomId(roomId);
        messageInfo.setAction(SocketClient.ACTION);
        messageInfo.setUserId(SocketClient.USER_ID);
        return messageInfo;
    }

    public void waitResponse(){
        while(StringUtils.isEmpty(SocketClient.IS_RESPONSE)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Thread error!", e);
            }
        }
        SocketClient.IS_RESPONSE = null;
    }
}

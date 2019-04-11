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
                if(SocketClient.USER_EXIST != null) {
                    if (SocketClient.USER_EXIST.equals("true")) {
                        messageInfo = showUnReadRoomHistory();
                        SocketClient.USER_EXIST = null;
                    } else {
                        System.out.println("用户不存在");
                        init();
                        SocketClient.CHOOSE_NO = "0";
                        return null;
                    }
                }
                if(SocketClient.ROOM_EXIST != null) {
                    if (SocketClient.ROOM_EXIST.equals("true")) {
                        messageInfo = showUnReadRoomHistory();
                        SocketClient.ROOM_EXIST = null;
                    } else {
                        System.out.println("聊天室不存在");
                        init();
                        SocketClient.CHOOSE_NO = "5";
                        return null;
                    }
                }
            }else if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[10])){
                messageInfo = showUnReadRoomHistory();
            } else if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[0])){
                messageInfo = completeSendMessageInfoByRoomId(SocketClient.ROOM_ID);
            }else if(SocketClient.ACTION.equals(SocketUtil.ACTIONS[1])){
                messageInfo = showUserHistory();
            }
        }else {
            if(StringUtils.isEmpty(SocketClient.CHOOSE_NO)) {
                System.out.println("选择序号(按#键加Enter返回到此选择)：" + SocketUtil.LINE_SEPARATOR + " 0." + SocketUtil.ACTIONS[0] + SocketUtil.LINE_SEPARATOR + " 1." + SocketUtil.ACTIONS[1] + SocketUtil.LINE_SEPARATOR + " 2." + SocketUtil.ACTIONS[2] + SocketUtil.LINE_SEPARATOR + " 3." + SocketUtil.ACTIONS[8] + SocketUtil.LINE_SEPARATOR + " 4." + SocketUtil.ACTIONS[9] + SocketUtil.LINE_SEPARATOR + " 5." + SocketUtil.ACTIONS[10] + SocketUtil.LINE_SEPARATOR + " 6." + SocketUtil.ACTIONS[12]);
                SocketClient.CHOOSE_NO = scanner.next();
            }
            switch (SocketClient.CHOOSE_NO) {
                case "0":
                    SocketClient.ACTION = SocketUtil.ACTIONS[7];
                    System.out.println("请输入对方用户名(按Enter键发送消息):");
                    String userIdTo = scanner.next();
                    if (userIdTo.equals("#")) {
                        init();
                        return null;
                    }
                    //check user
                    messageInfo = checkUser(userIdTo);
                    break;
                case "1":
                    SocketClient.ACTION = SocketUtil.ACTIONS[1];
                    String userId = null;
                    System.out.println("请输入对方用户名：");
                    userId = scanner.next();
                    if (userId.equals("#")) {
                        init();
                        return null;
                    }
                    //check user
                    messageInfo = checkUser(userId);
                    break;
                case "2":
                    messageInfo = new MessageInfo();
                    messageInfo.setAction(SocketUtil.ACTIONS[2]);
                    SocketClient.ACTION = SocketUtil.ACTIONS[2];
                    break;
                case "3":
                    System.out.println("创建聊天室（输入聊天室成员用户名,用户名之间用英文逗号分开：）");
                    String userIdsStr = scanner.next();
                    if (userIdsStr.equals("#")) {
                        init();
                        return null;
                    }
                    String[] userIds = userIdsStr.split(",");
                    messageInfo = new MessageInfo();
                    messageInfo.setUserId(SocketClient.USER_ID);
                    messageInfo.setUserIds(userIds);
                    messageInfo.setAction(SocketUtil.ACTIONS[8]);
                    break;
                case "4":
                    messageInfo = new MessageInfo();
                    messageInfo.setUserId(SocketClient.USER_ID);
                    messageInfo.setAction(SocketUtil.ACTIONS[9]);
                    break;
                case "5":
                    SocketClient.ACTION = SocketUtil.ACTIONS[7];
                    System.out.println("请输入roomId(按Enter键发送消息):");
                    String roomId = scanner.next();
                    if (roomId.equals("#")) {
                        init();
                        return null;
                    }
                    //check room
                    messageInfo = checkRoom(roomId);
                    break;
                case "6":
                    System.out.println("输入待加入聊天室的Id：");
                    String roomIdToAdd = scanner.next();
                    if (roomIdToAdd.equals("#")) {
                        init();
                        return null;
                    }
                    System.out.println("输入新加入的用户名,用户名之间用英文逗号分开：");
                    String userIdsToAddStr = scanner.next();
                    if (userIdsToAddStr.equals("#")) {
                        init();
                        return null;
                    }
                    String[] userIdsToAdd = userIdsToAddStr.split(",");
                    messageInfo = new MessageInfo();
                    messageInfo.setUserId(SocketClient.USER_ID);
                    messageInfo.setUserIds(userIdsToAdd);
                    messageInfo.setRoomId(roomIdToAdd);
                    messageInfo.setAction(SocketUtil.ACTIONS[12]);
                    break;
                default:
                    System.out.println("没有该选项，请重新选择!");
                    init();
                    break;
            }
        }
        return messageInfo;
    }

    public MessageInfo checkUser(String userId){
        Scanner scanner = new Scanner(System.in);
        while(userId.equals(SocketClient.USER_ID)){
            System.out.println("用户名不能为自己的用户名，请输入对方用户名：");
            userId = scanner.next();
            if (userId.equals("#")) {
                init();
                return null;
            }
        }
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[5]);
        messageInfo.setCheckUserId(userId);
        messageInfo.setUserId(SocketClient.USER_ID);
        return messageInfo;
    }

    public MessageInfo checkRoom(String roomId){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[11]);
        messageInfo.setRoomId(roomId);
        messageInfo.setUserId(SocketClient.USER_ID);
        return messageInfo;
    }

    public MessageInfo showUnReadRoomHistory(){
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
        return messageInfo;
    }

    public MessageInfo showUserHistory(){
        if(SocketClient.USER_EXIST.equals("true")) {
            MessageInfo messageInfo =completeViewHistoryMessageInfoByRoomId(SocketClient.ROOM_ID);
            SocketClient.USER_EXIST = null;
            SocketClient.ACTION = null;
            return messageInfo;
        }else{
            System.out.println("用户不存在");
            init();
            SocketClient.CHOOSE_NO = "1";
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
            init();
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

    public void init(){
        SocketClient.ACTION = null;
        SocketClient.ROOM_ID = null;
        SocketClient.IS_RESPONSE = "true";
        SocketClient.CHOOSE_NO = null;
    }
}

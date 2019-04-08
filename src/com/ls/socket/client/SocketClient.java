package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.client.entity.MessageReadMark;
import com.ls.socket.client.service.MessageReadMarkService;
import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.service.MessageInfoService;
import com.ls.socket.service.RoomUserService;
import com.ls.socket.service.UserService;
import com.ls.socket.util.FileUtil;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;

public class SocketClient {
    public static String ACTION = null;
    public static String ROOM_ID = null;
    public static String CLIENT_ID = null;
    private static Logger log = Logger.getLogger(SocketClient.class);
    public static void run(String userId, String dataPath, String ip, int port){
        if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(dataPath)){
            System.out.println("必须指定用户名和文件存储目录");
            log.error("必须指定用户名和文件存储目录");
            System.exit(0);
        }
        SocketClient socketClient = new SocketClient();
        socketClient.CLIENT_ID = userId;
        initDataFile(dataPath);
        Socket socket = socketClient.initClient(ip, port);
        if(socket!=null){
            //接收消息
            socketClient.receiveMessage(socket);
            //发送消息
            socketClient.sendMessage(socket);
        }
        //检测重连
//        new HeartBeatThread(socket, ip, port).start();
    }

    protected static Socket initClient(String ip, int port){
        Socket socket = null;
        PrintWriter writer = null;
        try {
            //客户端请求与服务器连接
            log.debug("客户端连接中...");
            socket = new Socket( ip, port);
            log.debug("客户端已连接");
            //获取Socket的输出流，用来发送数据到服务端
            writer = new PrintWriter(socket.getOutputStream());
            //绑定客户端信息
            bindInfoWithServer(CLIENT_ID, writer);
        }catch (IOException e){
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e1) {
                    log.error(e1.getMessage());
                }
            }
            if(writer != null) {
                writer.close();
            }
            log.debug("服务器未连接");
        }
        return socket;
    }

    //绑定clientId
    protected static void bindInfoWithServer(String clientId, PrintWriter writer){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[3]);
        //将clientId发送到服务端
        messageInfo.setClientId(clientId);
        writer.println(new Gson().toJson(messageInfo));
        writer.flush();
    }

    //开启线程接收信息
    protected static void receiveMessage(Socket socket){
        new ReceiveThread(socket).start();
    }

    //循环接收指令发送消息
    protected static void sendMessage(Socket socket){
        new SendThread(socket).start();
    }

    protected static MessageInfo initMessageInfo(){
        MessageInfo messageInfo = new MessageInfo();
        Scanner scanner = new Scanner(System.in);
        if(ACTION != null){
            messageInfo.setAction(ACTION);
            if(ACTION.equals(SocketUtil.ACTIONS[0])){
                messageInfo = completeSendMessageInfoByRoomId(ROOM_ID, messageInfo);
            }else if(ACTION.equals(SocketUtil.ACTIONS[1])){
                String printStr = "";
                while (!printStr.equals("#")) {
                    printStr = scanner.next();
                }
                ACTION = null;
                messageInfo = null;
            }else if(ACTION.equals(SocketUtil.ACTIONS[2])){
                String printStr = "";
                while (!printStr.equals("#")) {
                    printStr = scanner.next();
                }
                ACTION = null;
                messageInfo = null;
            }
        }else {
            System.out.println("选择序号：0." + SocketUtil.ACTIONS[0] + " 1." + SocketUtil.ACTIONS[1] + " 2." + SocketUtil.ACTIONS[2]);
            String orderNumber = scanner.next();
            switch (orderNumber) {
                case "0":
                    ACTION = SocketUtil.ACTIONS[0];
                    System.out.println("请输入好友用户名(按Enter键发送消息,按#键加Enter退出聊天):");
                    String friendClientId = scanner.next();
                    messageInfo = initSend(friendClientId);
                    //TODO
                    break;
                case "1":
                    System.out.println("请输入对方用户名(按#键加Enter键退出历史查询)：");
                    String friendId = scanner.next();
                    messageInfo = completeHistoryMessageInfo(friendId, messageInfo);
                    break;
                case "2":
                    messageInfo.setAction(SocketUtil.ACTIONS[2]);
                    ACTION = SocketUtil.ACTIONS[2];
                    break;
                default:
                    System.out.println("没有该选项，请重新选择!");
                    messageInfo = null;
                    break;
            }
        }
        return messageInfo;
    }

    //初始化发送消息（发送前检查用户是否存在，显示历史消息）
    protected static MessageInfo initSend(String friendId){
        boolean userIsExist = new UserService().checkUserIsExist(friendId);
        if(userIsExist) {
            RoomUserService roomUserService = new RoomUserService();
            String roomId = roomUserService.getSingleRoomIdByUserIds(CLIENT_ID, friendId);
            if (roomId == null) {
                //新建room,并关联用户
                ChatRoom room = roomUserService.createSingleChatRoom(CLIENT_ID, friendId);
                roomId = room.getRoomId();
            }
            ROOM_ID = roomId;
            MessageReadMarkService messageReadMarkService = new MessageReadMarkService();
            MessageReadMark messageReadMark = messageReadMarkService.getMessageReadMarkByRoomId(roomId);
            if (messageReadMark == null) {
                messageReadMark = new MessageReadMark();
                String messageMarkId = "0";
                messageReadMark.setRoomId(roomId);
                messageReadMark.setMessageId(messageMarkId);
                messageReadMarkService.saveMessageReadMark(messageReadMark);
            } else {
                List<MessageInfo> messageInfos = new MessageInfoService().getMessageInfosByRoomId(roomId);
                if (messageInfos.size() > 0) {
                    String historyStr = "";
                    String messageMarkId = messageReadMark.getMessageId();
                    int count = 0;
                    for (int i = 0; i < messageInfos.size(); i++) {
                        MessageInfo messageInfo = messageInfos.get(i);
                        if (Integer.parseInt(messageInfo.getMessageId()) > Integer.parseInt(messageMarkId) && !messageInfo.getClientId().equals(CLIENT_ID)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                            String dateStr = dateFormat.format(messageInfo.getDate());
                            String messageStr = dateStr + ": " + messageInfo.getClientId() + ": " + messageInfo.getMessageContent();
                            historyStr += messageStr + SocketUtil.LINE_SEPARATOR;
                            messageMarkId = messageInfo.getMessageId();
                            count ++;
                        }
                    }
                    if (count>0){
                        System.out.println(historyStr);
                        System.out.println("------以上是未浏览消息记录("+ count +"条)------"+SocketUtil.LINE_SEPARATOR);
                        messageReadMark.setMessageId(messageMarkId);
                        new MessageReadMarkService().saveMessageReadMark(messageReadMark);
                    }
                }
            }
            MessageInfo messageInfo = completeSendMessageInfoByRoomId(roomId, new MessageInfo());
            return messageInfo;
        }else{
            System.out.println("此用户不存在！");
            return null;
        }
    }


    protected static MessageInfo completeSendMessageInfoByRoomId(String roomId, MessageInfo messageInfo){
        messageInfo.setRoomId(roomId);
        messageInfo.setAction(ACTION);
        messageInfo.setClientId(CLIENT_ID);
        Scanner scanner = new Scanner(System.in);
        String messageContent = scanner.next();
        if (messageContent.equals("#")) {
            ACTION = null;
            ROOM_ID = null;
            return null;
        }
        messageInfo.setMessageContent(messageContent);
        return messageInfo;
    }

    protected static MessageInfo completeHistoryMessageInfo(String friendId, MessageInfo messageInfo){
        ACTION = SocketUtil.ACTIONS[1];
        String roomId = new RoomUserService().getSingleRoomIdByUserIds(CLIENT_ID, friendId);
        ROOM_ID = roomId;
        messageInfo.setAction(ACTION);
        messageInfo.setRoomId(roomId);
        return messageInfo;
    }

    protected static void initDataFile(String dataPath){
        MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH = dataPath + "/messageReadMark" + CLIENT_ID + ".txt";
        FileUtil.createFileIfNotExist(MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH);


        MessageInfoService.MESSAGE_FILE_PATH = dataPath + "/messageInfo.txt";
        RoomUserService.ROOM_FILE_PATH = dataPath + "/room.txt";
        RoomUserService.ROOM_USER_FILE_PATH = dataPath + "/roomUser.txt";
        UserService.USER_FILE_PATH = dataPath + "/user.txt";
        FileUtil.createFileIfNotExist(MessageInfoService.MESSAGE_FILE_PATH);
        FileUtil.createFileIfNotExist(RoomUserService.ROOM_FILE_PATH);
        FileUtil.createFileIfNotExist(RoomUserService.ROOM_USER_FILE_PATH);
        FileUtil.createFileIfNotExist(UserService.USER_FILE_PATH);
    }
}

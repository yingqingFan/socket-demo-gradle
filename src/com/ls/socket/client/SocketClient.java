package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.entity.MessageReadMark;
import com.ls.socket.service.MessageReadMarkService;
import com.ls.socket.util.FileUtil;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {
    public static String ACTION = null;
    public static String ROOM_ID = null;
    public static String CLIENT_ID = null;
    public static String USER_EXIST = null;
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
        new HeartBeatThread(socket, ip, port).start();
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
        MessageInfo messageInfo = null;
        Scanner scanner = new Scanner(System.in);
        if(ACTION != null){
            if(ACTION.equals(SocketUtil.ACTIONS[7])){
                messageInfo = showUnReadRoomHistory();
            } else if(ACTION.equals(SocketUtil.ACTIONS[0])){
                messageInfo = completeMessageInfoByRoomId(ROOM_ID);
            }else if(ACTION.equals(SocketUtil.ACTIONS[1])){
                messageInfo = showUserHistory();
            }
        }else {
            System.out.println("选择序号：0." + SocketUtil.ACTIONS[0] + " 1." + SocketUtil.ACTIONS[1] + " 2." + SocketUtil.ACTIONS[2]);
            String orderNumber = scanner.next();
            switch (orderNumber) {
                case "0":
                    ACTION = SocketUtil.ACTIONS[7];
                    System.out.println("请输入好友用户名(按Enter键发送消息,按#键加Enter退出聊天):");
                    String friendClientId = scanner.next();
                    //check user
                    messageInfo = checkUser(friendClientId);
                    break;
                case "1":
                    ACTION = SocketUtil.ACTIONS[1];
                    System.out.println("请输入对方用户名：");
                    String friendId = scanner.next();
                    //check user
                    messageInfo = checkUser(friendId);
                    break;
                case "2":
                    messageInfo = new MessageInfo();
                    messageInfo.setAction(SocketUtil.ACTIONS[2]);
                    break;
                default:
                    System.out.println("没有该选项，请重新选择!");
                    break;
            }
        }
        return messageInfo;
    }

    protected static MessageInfo checkUser(String userId){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[5]);
        messageInfo.setCheckFriendId(userId);
        messageInfo.setClientId(CLIENT_ID);
        return messageInfo;
    }

    protected static MessageInfo showUnReadRoomHistory(){
        while(StringUtils.isEmpty(USER_EXIST)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Thread error!", e);
            }
        }
        if(USER_EXIST.equals("true")){
            System.out.println("提示：已进入聊天室（光标处输入想要发送的消息，按Enter键发送）");
            MessageReadMarkService messageReadMarkService = new MessageReadMarkService();
            MessageReadMark messageReadMark = messageReadMarkService.getMessageReadMarkByRoomId(ROOM_ID);
            if (messageReadMark == null) {
                messageReadMark = new MessageReadMark();
                String messageMarkId = "0";
                messageReadMark.setRoomId(ROOM_ID);
                messageReadMark.setMessageId(messageMarkId);
                messageReadMarkService.saveMessageReadMark(messageReadMark);
            }
            //获取记录读取标记
            String messageMarkId = messageReadMark.getMessageId();
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setAction(SocketUtil.ACTIONS[7]);
            messageInfo.setRoomId(ROOM_ID);
            messageInfo.setMessageMarkId(messageMarkId);
            messageInfo.setClientId(CLIENT_ID);
            ACTION = SocketUtil.ACTIONS[0];
            USER_EXIST = null;
            return messageInfo;
        }else{
            System.out.println("用户不存在");
            USER_EXIST = null;
            ACTION = null;
            return null;
        }
    }

    protected static MessageInfo showUserHistory(){
        while(StringUtils.isEmpty(USER_EXIST)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Thread error!", e);
            }
        }
        if(USER_EXIST.equals("true")) {
            MessageInfo messageInfo = completeMessageInfoByRoomId(ROOM_ID);
            USER_EXIST = null;
            ACTION = null;
            return messageInfo;
        }else{
            System.out.println("用户不存在");
            USER_EXIST = null;
            ACTION = null;
            return null;
        }
    }

    protected static MessageInfo completeMessageInfoByRoomId(String roomId){
        MessageInfo messageInfo = new MessageInfo();
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

    protected static void initDataFile(String dataPath){
        MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH = dataPath + "/messageReadMark_" + CLIENT_ID + ".txt";
        FileUtil.createFileIfNotExist(MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH);
    }
}

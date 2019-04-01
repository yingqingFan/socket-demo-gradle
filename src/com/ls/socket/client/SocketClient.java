package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {
    public static final String[] ACTIONS = new String[]{"send message", "view history", "view online clients", "bind", "heartbeat"};
    public static String ACTION = null;
    public static String FRIEND_ClIENTID = null;
    public static String clientId = null;

    public static void run(String clientId, String ip, int port){
        if(StringUtils.isEmpty(clientId)){
            System.out.println("必须指定客户端Id");
            return;
        }
        SocketClient socketClient = new SocketClient();
        socketClient.clientId = clientId;
        Socket socket = socketClient.initClient(ip, port);
        if(socket!=null){
            //接收消息
            socketClient.receiveMessage(socket);
            //发送消息
            socketClient.sendMessage(socket);
        }
        //检测重连
        new ReconnectThread(socket, ip, port).start();
    }

    protected static Socket initClient(String ip, int port){
        Socket socket = null;
        PrintStream printStream = null;
        try {
            //客户端请求与服务器连接
            socket = new Socket( ip, port);
            //获取Socket的输出流，用来发送数据到服务端
            printStream = new PrintStream(socket.getOutputStream());
            //绑定客户端信息
            bindInfoWithServer(clientId, printStream);
        }catch (IOException e){
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if(printStream !=null){
                printStream.close();
            }
            System.out.println("服务器未连接");
        }
        return socket;
    }

    //绑定clientId
    protected static void bindInfoWithServer(String clientId, PrintStream out){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(ACTIONS[3]);
        //将clientId发送到服务端
        messageInfo.setClientId(clientId);
        out.println(new Gson().toJson(messageInfo));
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
        if(ACTION != null && ACTION != ACTIONS[2]){
            messageInfo.setAction(ACTION);
            if(ACTION.equals(ACTIONS[0])){
                if(FRIEND_ClIENTID != null){
                    messageInfo = completeSendMessageInfoById(FRIEND_ClIENTID,messageInfo);
                }else{
                    System.out.print("请输入好友clientId:");
                    String friendClientId = scanner.next();
                    messageInfo = completeSendMessageInfoById(friendClientId,messageInfo);
                }
            }else if(ACTION.equals(ACTIONS[1])){
                messageInfo = completeHistoryMessageInfo(messageInfo);
            }
        }else {
            System.out.println("请选择操作序号：0." + ACTIONS[0] + " 1." + ACTIONS[1] + " 2." + ACTIONS[2]);
            String orderNumber = scanner.next();
            switch (orderNumber) {
                case "0":
                    ACTION = ACTIONS[0];
                    messageInfo.setAction(ACTION);
                    System.out.print("请输入好友clientId:");
                    String friendClientId = scanner.next();
                    messageInfo = completeSendMessageInfoById(friendClientId, messageInfo);
                    break;
                case "1":
                    ACTION = ACTIONS[1];
                    messageInfo.setAction(ACTION);
                    messageInfo = completeHistoryMessageInfo(messageInfo);
                    break;
                case "2":
                    messageInfo.setAction(ACTIONS[2]);
                    break;
                default:
                    System.out.println("没有该选项，请重新选择!");
                    messageInfo = null;
                    break;
            }
        }
        return messageInfo;
    }

    protected static MessageInfo completeSendMessageInfoById(String friendId, MessageInfo messageInfo){
        FRIEND_ClIENTID = friendId;
        messageInfo.setFriendClientId(FRIEND_ClIENTID);
        System.out.println("To client" + FRIEND_ClIENTID + "(按Enter键发送消息,按#键和Enter退出聊天):");
        Scanner scanner = new Scanner(System.in);
        String messageContent = scanner.next();
        if (messageContent.equals("#")) {
            ACTION = null;
            return null;
        }
        messageInfo.setMessageContent(messageContent);
        return messageInfo;
    }

    protected static MessageInfo completeHistoryMessageInfo(MessageInfo messageInfo){
        System.out.println("你想查询和谁之间的历史记录，请输入对方clientId(按#键加Enter键退出历史查询)：");
        Scanner scanner = new Scanner(System.in);
        String friendClientId = scanner.next();
        if (friendClientId.equals("#")) {
            ACTION = null;
            return null;
        }
        messageInfo.setFriendClientId(friendClientId);
        return messageInfo;
    }
}

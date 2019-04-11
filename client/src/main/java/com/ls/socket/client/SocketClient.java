package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.service.MessageReadMarkService;
import com.ls.socket.util.FileUtil;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    public static String ACTION = null;
    public static String ROOM_ID = null;
    public static String USER_ID = null;
    public static String USER_EXIST = null;
    public static String IS_RESPONSE = null;
    public static SendThread sendThread = null;
    public static ReceiveThread receiveThread = null;
    public static String CHOOSE_NO = null;
    private static Logger log = Logger.getLogger(SocketClient.class);
    public void run(String userId, String dataPath, String ip, int port){
        if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(dataPath)){
            System.out.println("必须指定用户名和文件存储目录");
            log.error("必须指定用户名和文件存储目录");
            System.exit(0);
        }
        SocketClient socketClient = new SocketClient();
        socketClient.USER_ID = userId;
        initDataFile(dataPath);
        Socket socket = initClient(ip, port);
        if(socket!=null){
            //接收消息
            receiveMessage(socket);
            //发送消息
            sendMessage(socket);
        }
        //检测重连
        new HeartBeatThread(socket, ip, port).start();
    }

    public static Socket initClient(String ip, int port){
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
            bindInfoWithServer(USER_ID, writer);
        }catch (IOException e){
            log.error("IOException", e);
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e1) {
                    log.error("IOException", e1);
                }
            }
            if(writer != null) {
                writer.close();
            }
            log.debug("服务器未连接");
        }
        return socket;
    }

    //绑定userId
    public static void bindInfoWithServer(String userId, PrintWriter writer){
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setAction(SocketUtil.ACTIONS[3]);
        //将userId发送到服务端
        messageInfo.setUserId(userId);
        writer.println(new Gson().toJson(messageInfo));
        writer.flush();
    }

    //开启线程接收信息
    public static void receiveMessage(Socket socket){
        receiveThread = new ReceiveThread(socket);
        receiveThread.start();
    }

    //循环接收指令发送消息
    public static void sendMessage(Socket socket){
        sendThread = new SendThread(socket);
        sendThread.start();
    }

    public void initDataFile(String dataPath){
        MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH = dataPath + "/messageReadMark_" + USER_ID + ".txt";
        FileUtil.createFileIfNotExist(MessageReadMarkService.MESSAGE_READ_MARK_FILE_PATH);
    }
}

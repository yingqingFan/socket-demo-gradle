package com.ls.socket.server;

import com.ls.socket.service.MessageInfoService;
import com.ls.socket.service.RoomUserService;
import com.ls.socket.service.UserService;
import com.ls.socket.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SocketServer {
    private static Logger log = Logger.getLogger(SocketServer.class);
    protected static Map<String, Socket> socketMap = new HashMap<>();
    protected static Map<String, String> clientSocketMap = new HashMap<>();
    protected static Map<String, String> socketClientMap = new HashMap<>();
    public static void run(int port, String dataPath) throws IOException {
        if(StringUtils.isEmpty(dataPath)){
            log.error("必须指定数据存储位置");
            System.exit(0);
        }
        initDataFile(dataPath);
        SocketServer socketServer = new SocketServer();
        //创建一个通信类的对象
        ServerSocket server = new ServerSocket(port);
        //输出当前服务器的端口号
        log.debug("服务器创建成功，端口号：" + server.getLocalPort());
        //容纳三个线程的线程池
        Executor pool = Executors.newFixedThreadPool(100);
        boolean flag = true;
        while (flag) {
            //阻塞等待客户端连接
            Socket socket = server.accept();
            String socketId = null;
            //为每个客户端分配唯一id
            while(StringUtils.isEmpty(socketId)|| socketServer.socketMap.get(socketId)!= null){
                socketId =((int) (Math.random()*100))+"";
            }
            //将socket存入内存中
            socketServer.socketMap.put(socketId,socket);
            //new一个线程与客户端交互,server.accept()等待连接,pool执行线程
//            pool.execute(socketServer.new ServerThread(socket, socketId));
            new ServerThread(socket, socketId+"").start();
        }
    }

    protected static void initDataFile(String dataPath){
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

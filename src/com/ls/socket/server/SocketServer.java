package com.ls.socket.server;

import com.ls.socket.entity.MessageInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SocketServer {
    //key-value:socketId-socket
    protected static Map<String, Socket> socketMap = new HashMap<>();
    protected static Map<String, String> clientSocketMap = new HashMap<>();
    protected static Map<String, String> socketClientMap = new HashMap<>();
    protected static List<MessageInfo> messageHistoryList = new ArrayList<>();
    public static void run() throws IOException {
        SocketServer socketServer = new SocketServer();
        //创建一个通信类的对象
        ServerSocket server = new ServerSocket(9999);
        //输出当前服务器的端口号
        System.out.println("服务器创建成功，端口号：" + server.getLocalPort());
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
}

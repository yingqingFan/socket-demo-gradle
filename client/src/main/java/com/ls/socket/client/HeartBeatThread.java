package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.util.SocketUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class HeartBeatThread extends Thread{
    private static Logger log = Logger.getLogger(HeartBeatThread.class);
    private Socket socket;
    private String ip;
    private int port;

    public HeartBeatThread(Socket socket, String ip, int port) {
        this.socket = socket;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }
            sendHeartBeat();
        }

    }

    public void sendHeartBeat(){
        PrintWriter writer = null;
        if(socket != null){
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setUserId(SocketClient.USER_ID);
            messageInfo.setAction(SocketUtil.ACTIONS[4]);
            messageInfo.setMessageContent("心跳");
            try {
                writer = new PrintWriter(socket.getOutputStream());
                writer.println(new Gson().toJson(messageInfo));
                writer.flush();
            } catch (IOException e) {
                log.error("IOException",e);
                if(writer != null){
                    writer.close();
                }
                SocketClient.sendThread.stop();
                SocketClient.receiveThread.stop();
                reconnect(ip, port);
            }
        }else{
            SocketClient.sendThread.stop();
            SocketClient.receiveThread.stop();
            reconnect(ip, port);
        }
    }

    public void reconnect(String ip, int port){
        log.debug("尝试重新连接...");
        socket = SocketClient.initClient(ip, port);
        if(socket!=null){
            //接收消息
            SocketClient.receiveMessage(socket);
            //发送消息
            SocketClient.sendMessage(socket);
        }
    }
}

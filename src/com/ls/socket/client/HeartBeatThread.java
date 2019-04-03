package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
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
                log.error(e.getMessage());
            }
            sendHeartBeat();
        }

    }

    public void sendHeartBeat(){
        PrintStream printStream = null;
        if(socket != null){
            try {
                printStream = new PrintStream(socket.getOutputStream());
                MessageInfo messageInfo = new MessageInfo();
                messageInfo.setClientId(SocketClient.CLIENT_ID);
                messageInfo.setAction(SocketClient.ACTIONS[4]);
                messageInfo.setMessageContent("心跳");
                printStream.println(new Gson().toJson(messageInfo));
                printStream.flush();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                   log.error(e1.getMessage());
                }
                if(printStream!=null) {
                    printStream.close();
                }
                reconnect(ip, port);
            }
        }else{
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

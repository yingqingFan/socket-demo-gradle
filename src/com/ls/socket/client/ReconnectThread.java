package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class ReconnectThread extends Thread{
    private Socket socket;
    public ReconnectThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendHeartBeat();
            System.out.println("################");
        }

    }

    public void sendHeartBeat(){
        PrintStream printStream = null;
        if(socket != null){
            try {
                printStream = new PrintStream(socket.getOutputStream());
                MessageInfo messageInfo = new MessageInfo();
                messageInfo.setClientId(SocketClient.clientId);
                messageInfo.setAction(SocketClient.ACTIONS[4]);
                messageInfo.setMessageContent("心跳");
                printStream.println(new Gson().toJson(messageInfo));
                printStream.flush();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if(printStream!=null) {
                    printStream.close();
                }
                reconnect();
            }
        }else{
            reconnect();
        }
    }

    public void reconnect(){
        System.out.println("尝试重新连接...");
        socket = SocketClient.initClient();
        if(socket!=null){
            //接收消息
            SocketClient.receiveMessage(socket);
            //发送消息
            SocketClient.sendMessage(socket);
        }
    }
}

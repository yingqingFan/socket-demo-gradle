package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;

public class SendThread extends Thread{
    private static Logger log = Logger.getLogger(SendThread.class);
    private Socket socket;
    public SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while(true) {
            MessageInfo messageInfo = SocketClient.initMessageInfo();
            if(messageInfo == null){
                continue;
            }
            messageInfo.setDate(new Date());
            String str = new Gson().toJson(messageInfo);
            //发送数据到服务端
            PrintStream printStream = null;
            try {
                printStream = new PrintStream(socket.getOutputStream());
                printStream.println(str);
            }catch (Exception e){
                if(printStream != null){
                    printStream.close();
                }
                return;
            }
        }

    }
}

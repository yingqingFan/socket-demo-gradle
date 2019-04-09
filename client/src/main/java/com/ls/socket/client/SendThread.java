package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

public class SendThread extends Thread{
    private static Logger log = Logger.getLogger(SendThread.class);
    private Socket socket;
    private PrintWriter writer;
    public SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream());
            while (true) {
                MessageInfo messageInfo = SocketClient.initMessageInfo();
                if (messageInfo == null) {
                    continue;
                }
                messageInfo.setDate(new Date());
                String str = new Gson().toJson(messageInfo);
                //发送数据到服务端
                writer.println(str);
                writer.flush();
            }
        }catch (IOException e) {
            log.error("IOException",e);
            try {
                socket.close();
            } catch (IOException e1) {
                log.error("IOException",e1);
            }
            if(writer != null) {
               writer.close();
            }
            return;
        }

    }
}

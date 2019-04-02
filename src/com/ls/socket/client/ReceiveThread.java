package com.ls.socket.client;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReceiveThread extends Thread{
    private static Logger log = Logger.getLogger(ReceiveThread.class);
    private Socket socket;
    public ReceiveThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader bufferedReader = null;
        try {
            while(true) {
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = null;
                while (((line = bufferedReader.readLine()) != null)) {
                    MessageInfo messageInfo = new Gson().fromJson(line, MessageInfo.class);
                    if(messageInfo.getAction()!=null && messageInfo.getAction().equals(SocketClient.ACTIONS[0])){
                        if(StringUtils.isEmpty(SocketClient.FRIEND_ClIENTID)){
                            System.out.println(messageInfo.getMessageContent());
                        }else if(messageInfo.getClientId().equals(SocketClient.FRIEND_ClIENTID)){
                            System.out.println(messageInfo.getMessageContent());
                        }
                    }else {
                        System.out.println(messageInfo.getMessageContent());
                    }
                }
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                log.error(e1.getMessage());
            }
            if(bufferedReader!=null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    log.error(e1.getMessage());
                }
            }
            log.debug("无法连接服务器");
            return;
        }
    }
}

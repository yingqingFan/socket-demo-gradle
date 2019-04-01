package com.ls.socket.client;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;

public class ClientTest {
    public static void main(String[] args) throws IOException {
        if(ArrayUtils.isEmpty(args)){
            System.out.println("必须指定客户端Id");
            return;
        }
        String clientId = args[0];
        SocketClient.run(clientId,"localhost", 9999);
    }
}

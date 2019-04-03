package com.ls.socket.client;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ClientTest {
    private static Logger log = Logger.getLogger(ClientTest.class);
    public static void main(String[] args) throws IOException {
        if(ArrayUtils.isEmpty(args)){
            log.error("必须指定用户名");
            System.exit(0);
        }
        String clientId = args[0];
        SocketClient.run(clientId,"localhost", 9999);
    }
}

package com.ls.socket.server;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ServerStart {
    private static Logger log = Logger.getLogger(ServerStart.class);
    public static void main(String[] args) {
        if(ArrayUtils.isEmpty(args)){
            log.error("必须指定数据存储位置");
            System.exit(0);
        }
        String path = args[0];
        try {
            SocketServer.run(9999, path);
        } catch (IOException e) {
            log.error("Server run error", e);
        }
    }
}

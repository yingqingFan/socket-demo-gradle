package com.ls.socket.client;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public class ClientStart {
    private static Logger log = Logger.getLogger(ClientStart.class);
    public static void main(String[] args) {
        if(ArrayUtils.isEmpty(args) || (args!=null && args.length<1)){
            log.error("必须指定用户名和文件存储目录");
            System.exit(0);
        }
        String userId = args[0];
        String dataPath = args[1];
        SocketClient.run(userId, dataPath,"localhost", 9999);
    }
}

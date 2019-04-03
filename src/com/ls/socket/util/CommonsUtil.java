package com.ls.socket.util;

import com.ls.socket.server.SocketServer;

public class CommonsUtil {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String DATA_FILE_PATH = SocketServer.class.getResource("/").getPath()+"messageInfo.txt";
}

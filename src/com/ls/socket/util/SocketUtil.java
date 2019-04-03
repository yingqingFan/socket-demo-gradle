package com.ls.socket.util;

import com.ls.socket.server.SocketServer;

public class SocketUtil {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String DATA_FILE_PATH = SocketServer.class.getResource("/").getPath()+"messageInfo.txt";
    public static final String[] ACTIONS = new String[]{"send message", "view history", "view online clients", "bind", "heartbeat", "init send", "power down"};
}

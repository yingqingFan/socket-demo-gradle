package com.ls.socket.server;

import com.google.gson.Gson;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.util.SocketUtil;
import com.ls.socket.util.DataUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ServerThread extends Thread {
    private static Logger log = Logger.getLogger(ServerThread.class);
    private PrintWriter writer;//输出流
    private BufferedReader bufferedReader;//输入流
    private Socket socket;
    private String socketId;

    public ServerThread(Socket socket, String socketId) {
        this.socket = socket;
        this.socketId = socketId;
    }

    @Override
    public void run() {
        //客户端连接后获取socket输出输入流
        try {
            writer = new PrintWriter(socket.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //读取客户端信息并转发
            readAndSend();
        } catch (IOException e) {
            log.error("error",e);
        }
    }

    //输出信息到指定客户端
    private void out(String outS, String socketId) {
        try {
            Socket socket1 = SocketServer.socketMap.get(socketId);
            if(socket1!=null) {
                PrintWriter printWriter1 = new PrintWriter(socket1.getOutputStream());
                //将信息发送客户机
                printWriter1.println(outS);
                //强制输出到命令行的界面中
                printWriter1.flush();
            }else{
                //提示客户端指定客户端不存在
                writer.println("目标用户不存在,请按#号键退出重新选择！");
                //强制输出到命令行的界面中
                writer.flush();
            }
        }catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    //输出信息到其他所有客户端
    public void outOthers(String outS) {
        Iterator<String> idIterator = SocketServer.socketMap.keySet().iterator();
        while(idIterator.hasNext()){
            String sId = idIterator.next();
            if(!sId.equals(socketId)) {
                out(outS, sId);
            }
        }
    }

    public void readAndSend() {
        String line = null;
        try {
            while (true) {
                while (((line = bufferedReader.readLine()) != null)) {
                    MessageInfo messageInfo = new Gson().fromJson(line, MessageInfo.class);
                    //如果是绑定信息
                    if(messageInfo.getAction().equals(SocketUtil.ACTIONS[3])){
                        bindClient(messageInfo);
                    }
                    //初始化发送消息
                    if(messageInfo.getAction().equals(SocketUtil.ACTIONS[5])){
                       initSendMessage(messageInfo);
                    }
                    //发送消息
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[0])) {
                        sendMessage(messageInfo);
                    }
                    //如果客户端是查看聊天历史记录，返回历史记录给客户端
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[1])){
                       outHistoryToClient(messageInfo);
                    }
                    //如果客户端是查看在线用户，返回在线用户给客户端
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[2])){
                        viewUsersOnline(messageInfo);
                    }
                    //心跳检测
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[4])){
                        heartBeat(messageInfo);
                    }
                }
            }
        }catch (IOException e) {
            log.error("error",e);
            SocketServer.socketMap.remove(socketId);
            String clientId = SocketServer.socketClientMap.get(socketId);
            SocketServer.socketClientMap.remove(socketId);
            SocketServer.clientSocketMap.remove(clientId);
            String outS = "client" + clientId + " 已下线";
            System.out.println("client" + clientId + " 断开连接");
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setMessageContent(outS);
            outOthers(new Gson().toJson(messageInfo));
        }
    }

    //绑定客户端
    public void bindClient(MessageInfo messageInfo){
        String clientId = messageInfo.getClientId();
        if(StringUtils.isEmpty(SocketServer.clientSocketMap.get(clientId))) {
            SocketServer.socketClientMap.put(socketId, clientId);
            SocketServer.clientSocketMap.put(clientId, socketId);
            //客户端上线成功提示
            String successMessage = "当前client" + clientId + " 上线成功";
            messageInfo.setMessageContent(successMessage);
            String successInfo = new Gson().toJson(messageInfo);
            out(successInfo, socketId);

            //告诉其他客户端当前客户端上线
            String outS = clientId + " 已上线";
            messageInfo.setMessageContent(outS);
            String infoToOthers = new Gson().toJson(messageInfo);
            outOthers(infoToOthers);
        }else{
            messageInfo.setMessageContent("用户名‘" + clientId + "’已被使用，请更换用户名后重新启动！");
            messageInfo.setAction(SocketUtil.ACTIONS[6]);
            String failInfo = new Gson().toJson(messageInfo);
            out(failInfo, socketId);
        }
    }

    //初始化发送消息（开启会话，显示发送历史）
    public void initSendMessage(MessageInfo messageInfo){
        String friendSocketId = SocketServer.clientSocketMap.get(messageInfo.getFriendClientId());
        Socket socket1 = SocketServer.socketMap.get(friendSocketId);
        if(socket1==null) {
            //提示客户端指定客户端不存在
            messageInfo.setMessageContent("目标用户不存在,请按#键加Enter退出重新选择！");
            out(new Gson().toJson(messageInfo),socketId);
        }else{
            outHistoryToClient(messageInfo);
        }
    }

    //发送消息
    public void sendMessage(MessageInfo messageInfo){
        String clientId = SocketServer.socketClientMap.get(socketId);
        messageInfo.setClientId(clientId);
        String clientIdTo = messageInfo.getFriendClientId();
        String message = messageInfo.getMessageContent();
        messageInfo.setMessageContent(clientId + ":" + message);
        //发送信息给目标客户端
        out(new Gson().toJson(messageInfo), SocketServer.clientSocketMap.get(clientIdTo));
        //将messageInfo存入本地文件
        messageInfo.setMessageContent(message);
        new DataUtil<MessageInfo>().writeToFile(SocketUtil.DATA_FILE_PATH,messageInfo);
    }

    public void outHistoryToClient(MessageInfo messageInfo){
        String historyStr = "";
        //将历史记录按时间排序
        List<MessageInfo> messageHistoryList = sortHistoryByTime();
        if(messageHistoryList.size()>0) {
            for (int i = 0; i < messageHistoryList.size(); i++) {
                MessageInfo sendHistory = messageHistoryList.get(i);
                if ((sendHistory.getClientId().equals(SocketServer.socketClientMap.get(socketId)) && sendHistory.getFriendClientId().equals(messageInfo.getFriendClientId()))
                        || (sendHistory.getClientId().equals(messageInfo.getFriendClientId()) && sendHistory.getFriendClientId().equals(SocketServer.socketClientMap.get(socketId)))) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    String dateStr = dateFormat.format(sendHistory.getDate());
                    String sendHistoryClientId = sendHistory.getClientId();
                    String clientId = SocketServer.socketClientMap.get(socketId);
                    if(sendHistoryClientId.equals(clientId)){
                        sendHistoryClientId = sendHistoryClientId+"(我)";
                    }
                    String messageStr = dateStr + ": " + sendHistoryClientId + ": " + sendHistory.getMessageContent();
                    historyStr += messageStr + SocketUtil.LINE_SEPARATOR;

                }
            }
            if(!StringUtils.isEmpty(historyStr)){
                historyStr += "---------- 以上为历史消息 ----------" + SocketUtil.LINE_SEPARATOR;
            }else{
                historyStr += "---------- 无历史消息 ----------" + SocketUtil.LINE_SEPARATOR;
            }
        }else{
            historyStr += "---------- 无历史消息 ----------" + SocketUtil.LINE_SEPARATOR;
        }
        //输出历史到客户端
        messageInfo.setMessageContent(historyStr);
        out(new Gson().toJson(messageInfo), socketId);
    }

    //将历史记录按时间排序
    public List<MessageInfo> sortHistoryByTime(){
        List<MessageInfo> messageHistoryList = new DataUtil<MessageInfo>().readFromFile(SocketUtil.DATA_FILE_PATH,MessageInfo.class);
        if(messageHistoryList.size()>0) {
            Collections.sort(messageHistoryList, new Comparator<MessageInfo>() {
                @Override
                public int compare(MessageInfo o1, MessageInfo o2) {
                    if (o1.getDate().before(o2.getDate())) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        }
        return messageHistoryList;
    }

    //查看当前在线用户
    public void viewUsersOnline(MessageInfo messageInfo){
        String usersOnline = "";
        Iterator<String> idIterator = SocketServer.socketMap.keySet().iterator();
        while(idIterator.hasNext()){
            String sId = idIterator.next();
            if(!sId.equals(socketId)) {
                String clientId = SocketServer.socketClientMap.get(sId);
                usersOnline += clientId + SocketUtil.LINE_SEPARATOR;
            }
        }
        if(StringUtils.isEmpty(usersOnline)){
            usersOnline += "######当前没有好友在线(按#键加Enter键退出查询)######" + SocketUtil.LINE_SEPARATOR;
        }else{
            usersOnline = "######以下是当前在线用户(按#键加Enter键退出查询)######" + SocketUtil.LINE_SEPARATOR + usersOnline;
        }
        messageInfo.setMessageContent(usersOnline);
        out(new Gson().toJson(messageInfo), socketId);
    }

    //心跳
    public void heartBeat(MessageInfo messageInfo){
        String clientId = messageInfo.getClientId();
        String socketId = SocketServer.clientSocketMap.get(clientId);
        String heartBeatMessage = messageInfo.getMessageContent();
        System.out.println(clientId + ":" + heartBeatMessage);
//        out(new Gson().toJson(messageInfo),socketId);
    }
}

package com.ls.socket.server;

import com.google.gson.Gson;
import com.ls.socket.client.SocketClient;
import com.ls.socket.entity.MessageInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class ServerThread extends Thread {
    Gson gson = new Gson();
    public static String lineSeparator = System.getProperty("line.separator");
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
        System.out.println("thread socketId:"+Thread.currentThread().getId());
        //客户端连接后获取socket输出输入流
        try {
            writer = new PrintWriter(socket.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //读取客户端信息并转发
            readAndSend();
        } catch (IOException e) {
            e.printStackTrace();
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
                PrintWriter printWriter1 = new PrintWriter(socket.getOutputStream());
                //提示客户端指定客户端不存在
                printWriter1.println("目标用户不存在,请按#号键退出重新选择！");
                //强制输出到命令行的界面中
                printWriter1.flush();
            }
        }catch (IOException e) {
            e.printStackTrace();
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
                    System.out.println("内容 :" + line);
                    MessageInfo messageInfo = gson.fromJson(line, MessageInfo.class);
                    //如果是绑定信息
                    if(messageInfo.getAction().equals(SocketClient.ACTIONS[3])){
                        String clientId = messageInfo.getClientId();
                        SocketServer.socketClientMap.put(socketId,clientId);
                        SocketServer.clientSocketMap.put(clientId,socketId);
                        //客户端上线成功提示
                        String successMessage =  "当前client" + clientId + " 上线成功";
                        messageInfo.setMessageContent(successMessage);
                        String successInfo = gson.toJson(messageInfo);
                        out(successInfo, socketId);

                        //告诉其他客户端当前客户端上线
                        String outS = clientId + " 已上线";
                        messageInfo.setMessageContent(outS);
                        String infoToOthers = gson.toJson(messageInfo);
                        outOthers(infoToOthers);
                    }
                    //初始化发送消息
                    if(messageInfo.getAction().equals(SocketClient.ACTIONS[5])){
                        String friendSocketId = SocketServer.clientSocketMap.get(messageInfo.getFriendClientId());
                        Socket socket1 = SocketServer.socketMap.get(friendSocketId);
                        if(socket1==null) {
                            //提示客户端指定客户端不存在
                            messageInfo.setMessageContent("目标用户不存在,请按#号键退出重新选择！");
                            out(gson.toJson(messageInfo),socketId);
                        }else{
                            outHistoryToClient(messageInfo);
                        }
                    }
                    //发送消息
                    else if(messageInfo.getAction().equals(SocketClient.ACTIONS[0])) {
                        String clientId = SocketServer.socketClientMap.get(socketId);
                        messageInfo.setClientId(clientId);
                        String clientIdTo = messageInfo.getFriendClientId();
                        String message = messageInfo.getMessageContent();
                        messageInfo.setMessageContent(clientId + ":" + message);
                        //发送信息给目标客户端
                        out(gson.toJson(messageInfo), SocketServer.clientSocketMap.get(clientIdTo));
                        //将messageInfo存入内存
                        messageInfo.setMessageContent(message);
                        SocketServer.messageHistoryList.add(messageInfo);
                    }
                    //如果客户端是查看聊天历史记录，返回历史记录给客户端
                    else if(messageInfo.getAction().equals(SocketClient.ACTIONS[1])){
                       outHistoryToClient(messageInfo);
                    }
                    //如果客户端是查看在线用户，返回在线用户给客户端
                    else if(messageInfo.getAction().equals(SocketClient.ACTIONS[2])){
                        String usersOnline = "";
                        Iterator<String> idIterator = SocketServer.socketMap.keySet().iterator();
                        while(idIterator.hasNext()){
                            String sId = idIterator.next();
                            String clientId = SocketServer.socketClientMap.get(sId);
                            usersOnline += clientId + lineSeparator;
                        }
                        messageInfo.setMessageContent(usersOnline);
                        out(gson.toJson(messageInfo), socketId);
                    }else if(messageInfo.getAction().equals(SocketClient.ACTIONS[4])){
                        String clientId = messageInfo.getClientId();
                        String socketId = SocketServer.clientSocketMap.get(clientId);
                        String heartBeatMessage = messageInfo.getMessageContent();
                        System.out.println(clientId + ":" + heartBeatMessage);
//                        out(gson.toJson(messageInfo),socketId);
                    }
                }
            }
        }catch (IOException e) {
            SocketServer.socketMap.remove(socketId);
            String clientId = SocketServer.socketClientMap.get(socketId);
            SocketServer.socketClientMap.remove(socketId);
            SocketServer.clientSocketMap.remove(clientId);
            String outS = "client" + clientId + " 已下线";
            System.out.println("client" + clientId + " 断开连接");
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setMessageContent(outS);
            outOthers(gson.toJson(messageInfo));
        }
    }

    public void outHistoryToClient(MessageInfo messageInfo){
        String historyStr = "";
        if(SocketServer.messageHistoryList.size()>0) {
            //将历史记录按时间排序
            Collections.sort(SocketServer.messageHistoryList, new Comparator<MessageInfo>() {
                @Override
                public int compare(MessageInfo o1, MessageInfo o2) {
                    if (o1.getDate().before(o2.getDate())) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            for (int i = 0; i < SocketServer.messageHistoryList.size(); i++) {
                MessageInfo sendHistory = SocketServer.messageHistoryList.get(i);
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
                    historyStr += messageStr + lineSeparator;

                }
            }
            if(!StringUtils.isEmpty(historyStr)){
                historyStr+="---------- 以上为历史消息 ----------"+lineSeparator;
            }else{
                historyStr+="---------- 无历史消息 ----------"+lineSeparator;
            }
        }else{
            historyStr+="---------- 无历史消息 ----------"+lineSeparator;
        }
        //输出历史到客户端
        messageInfo.setMessageContent(historyStr);
        out(gson.toJson(messageInfo), socketId);
    }
}

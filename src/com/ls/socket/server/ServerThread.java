package com.ls.socket.server;

import com.google.gson.Gson;
import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.entity.User;
import com.ls.socket.service.MessageInfoService;
import com.ls.socket.service.RoomUserService;
import com.ls.socket.service.UserService;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
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
                    //检查用户是否存在
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[5])){
                        checkUser(messageInfo);
                    }
                    //展示未读聊天室历史记录
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[7])){
                       showUnreadRoomHistory(messageInfo);
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
            String userId = SocketServer.socketUserMap.get(socketId);
            SocketServer.socketUserMap.remove(socketId);
            SocketServer.userSocketMap.remove(userId);
            String outS = "用户：" + userId + " 已下线";
            System.out.println("用户：" + userId + " 断开连接");
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setMessageContent(outS);
            outOthers(new Gson().toJson(messageInfo));
        }
    }

    //绑定客户端
    public void bindClient(MessageInfo messageInfo){
        String userId = messageInfo.getUserId();
        if(StringUtils.isEmpty(SocketServer.userSocketMap.get(userId))) {
            SocketServer.socketUserMap.put(socketId, userId);
            SocketServer.userSocketMap.put(userId, socketId);
            UserService userService =  new UserService();
            boolean userIsExsit = userService.checkUserIsExist(userId);
            if(!userIsExsit){
                User user = new User();
                user.setUserId(userId);
                userService.saveUser(user);
            }
            //客户端上线成功提示
            String successMessage = "当前用户：" + userId + " 上线成功";
            messageInfo.setMessageContent(successMessage);
            String successInfo = new Gson().toJson(messageInfo);
            out(successInfo, socketId);

            //告诉其他客户端当前客户端上线
            String outS = userId + " 已上线";
            messageInfo.setMessageContent(outS);
            String infoToOthers = new Gson().toJson(messageInfo);
            outOthers(infoToOthers);
        }
    }

    //check user is exist
    public void checkUser(MessageInfo messageInfo){
        boolean userIsExist = new UserService().checkUserIsExist(messageInfo.getCheckUserId());
        if(userIsExist){
            RoomUserService roomUserService = new RoomUserService();
            String roomId = roomUserService.getSingleRoomIdByUserIds(messageInfo.getUserId(), messageInfo.getCheckUserId());
            if (roomId == null) {
                //新建room,并关联用户
                ChatRoom room = roomUserService.createSingleChatRoom(messageInfo.getUserId(), messageInfo.getCheckUserId());
                roomId = room.getRoomId();
            }
            messageInfo.setRoomId(roomId);
        }
        out(new Gson().toJson(messageInfo), socketId);
    }

    public void showUnreadRoomHistory(MessageInfo messageInfo){
        List<MessageInfo> messageInfos = new MessageInfoService().getMessageInfosByRoomId(messageInfo.getRoomId());
        if (messageInfos.size() > 0) {
            String historyStr = "";
            String messageMarkId = messageInfo.getMessageMarkId();
            int count = 0;
            for (int i = 0; i < messageInfos.size(); i++) {
                MessageInfo messageInfo1 = messageInfos.get(i);
                if (Integer.parseInt(messageInfo1.getMessageId()) > Integer.parseInt(messageMarkId) && !messageInfo1.getUserId().equals(messageInfo.getUserId())) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    String dateStr = dateFormat.format(messageInfo1.getDate());
                    String messageStr = dateStr + ": " + messageInfo1.getUserId() + ": " + messageInfo1.getMessageContent();
                    historyStr += messageStr + SocketUtil.LINE_SEPARATOR;
                    messageMarkId = messageInfo1.getMessageId();
                    messageInfo.setMessageMarkId(messageMarkId);
                    count ++;
                }
            }
            if (count>0){
                historyStr += "------以上是未浏览消息记录("+ count +"条)------"+SocketUtil.LINE_SEPARATOR;
                messageInfo.setMessageContent(historyStr);
                out(new Gson().toJson(messageInfo), socketId);
            }
        }
    }

    //发送消息
    public void sendMessage(MessageInfo messageInfo){
        //将messageInfo存入本地文件
        messageInfo = new MessageInfoService().saveMessageInfo(messageInfo);
        String roomId = messageInfo.getRoomId();
        if(roomId != null) {
            RoomUserService roomUserService = new RoomUserService();
            List<String> userIds = roomUserService.getUserIdsByRoomId(roomId);
            if(userIds != null && userIds.size()>0){
                for (int i = 0; i < userIds.size(); i++) {
                    if(!userIds.get(i).equals(messageInfo.getUserId())) {
                        String friendSocketId = SocketServer.userSocketMap.get(userIds.get(i));
                        Socket socket1 = SocketServer.socketMap.get(friendSocketId);
                        if (socket1 != null) {
                            String userId = SocketServer.socketUserMap.get(socketId);
                            messageInfo.setUserId(userId);
                            String message = messageInfo.getMessageContent();
                            messageInfo.setMessageContent(userId + ":" + message);
                            //发送信息给目标客户端
                            out(new Gson().toJson(messageInfo), friendSocketId);
                        }
                    }
                }
            }
        }
    }

    public void outHistoryToClient(MessageInfo messageInfo){
        String historyStr = "";
        String roomId = new RoomUserService().getSingleRoomIdByUserIds(messageInfo.getUserId(), messageInfo.getCheckUserId());
        //根据roomId获取消息记录
        List<MessageInfo> messageHistoryList = new MessageInfoService().getMessageInfosByRoomId(roomId);
        if(messageHistoryList.size()>0) {
            for (int i = 0; i < messageHistoryList.size(); i++) {
                MessageInfo sendHistory = messageHistoryList.get(i);
                if(sendHistory != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    String dateStr = dateFormat.format(sendHistory.getDate());
                    String sendHistoryUserId = sendHistory.getUserId();
                    String userId = SocketServer.socketUserMap.get(socketId);
                    if (sendHistoryUserId.equals(userId)) {
                        sendHistoryUserId = sendHistoryUserId + "(我)";
                    }
                    String messageStr = dateStr + ": " + sendHistoryUserId + ": " + sendHistory.getMessageContent();
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

    //查看当前在线用户
    public void viewUsersOnline(MessageInfo messageInfo){
        String usersOnline = "";
        Iterator<String> idIterator = SocketServer.socketMap.keySet().iterator();
        while(idIterator.hasNext()){
            String sId = idIterator.next();
            if(!sId.equals(socketId)) {
                String userId = SocketServer.socketUserMap.get(sId);
                usersOnline += userId + SocketUtil.LINE_SEPARATOR;
            }
        }
        if(StringUtils.isEmpty(usersOnline)){
            usersOnline += "######当前没有用户在线######" + SocketUtil.LINE_SEPARATOR;
        }else{
            usersOnline = "######以下是当前在线用户######" + SocketUtil.LINE_SEPARATOR + usersOnline;
        }
        messageInfo.setMessageContent(usersOnline);
        out(new Gson().toJson(messageInfo), socketId);
    }

    //心跳
    public void heartBeat(MessageInfo messageInfo){
        String userId = messageInfo.getUserId();
        String socketId = SocketServer.userSocketMap.get(userId);
        String heartBeatMessage = messageInfo.getMessageContent();
        System.out.println(userId + ":" + heartBeatMessage);
//        out(new Gson().toJson(messageInfo),socketId);
    }
}

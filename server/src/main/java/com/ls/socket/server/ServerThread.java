package com.ls.socket.server;

import com.google.gson.Gson;
import com.ls.socket.entity.ChatRoom;
import com.ls.socket.entity.MessageInfo;
import com.ls.socket.entity.RoomUser;
import com.ls.socket.entity.User;
import com.ls.socket.service.MessageInfoService;
import com.ls.socket.service.RoomService;
import com.ls.socket.service.RoomUserService;
import com.ls.socket.service.UserService;
import com.ls.socket.util.SocketUtil;
import org.apache.commons.lang3.ArrayUtils;
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
    private PrintWriter writer;
    private BufferedReader bufferedReader;
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
            log.error("IOException",e);
        }
    }

    //输出信息到指定客户端
    private void out(String outS, String socketId) {
        try {
            Socket socket1 = SocketServer.socketMap.get(socketId);
            if(socket1!=null) {
                PrintWriter printWriter1 = new PrintWriter(socket1.getOutputStream());
                //将信息发送到客户机
                printWriter1.println(outS);
                //强制输出到命令行的界面中
                printWriter1.flush();
            }
        }catch (IOException e) {
            log.error("IOException", e);
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
                    //检查群聊聊天室是否存在
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[11])){
                        checkRoom(messageInfo);
                    }
                    //展示未读聊天室历史记录
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[7])){
                       showUnreadRoomHistory(messageInfo);
                    }
                    //发送消息
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[0]) || messageInfo.getAction().equals(SocketUtil.ACTIONS[10])) {
                        sendMessage(messageInfo);
                    }
                    //如果客户端是查看聊天历史记录，返回历史记录给客户端
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[1]) || messageInfo.getAction().equals(SocketUtil.ACTIONS[13])){
                       outHistoryToClient(messageInfo);
                    }
                    //如果客户端是查看在线用户，返回在线用户给客户端
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[2])){
                        viewUsersOnline(messageInfo);
                    }
                    //如果是创建群聊聊天室
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[8])){
                        createRoom(messageInfo);
                    }
                    //如果是查看群聊聊天室列表
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[9])){
                        viewRoomsList(messageInfo);
                    }
                    //向群聊聊天室添加用户
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[12])){
                        addUsersToRoom(messageInfo);
                    }
                    //退出群聊
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[14])){
                        leaveGroupRoom(messageInfo);
                    }
                    //心跳检测
                    else if(messageInfo.getAction().equals(SocketUtil.ACTIONS[4])){
                        heartBeat(messageInfo);
                    }
                }
            }
        }catch (Exception e) {
            log.error("error",e);
            String userId = SocketServer.socketUserMap.get(socketId);
            //bindClient失败时socket没有对应userId，无需显示下线通知
            if(!StringUtils.isEmpty(userId)) {
                MessageInfo messageInfo = new MessageInfo();
                messageInfo.setMessageContent("服务器发生异常，当前用户断开连接");
                messageInfo.setAction(SocketUtil.ACTIONS[6]);
                out(new Gson().toJson(messageInfo),socketId);
                String outS = "提示： " + userId + " 已下线";
                System.out.println("提示： " + userId + " 断开连接");
                messageInfo.setMessageContent(outS);
                messageInfo.setAction(null);
                outOthers(new Gson().toJson(messageInfo));
                SocketServer.userSocketMap.remove(userId);
            }
            SocketServer.socketMap.remove(socketId);
            SocketServer.socketUserMap.remove(socketId);
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
            String successMessage = "提示： 当前用户 " + userId + " 上线成功";
            System.out.println("提示： " + userId + " 已连接");
            messageInfo.setMessageContent(successMessage);
            String successInfo = new Gson().toJson(messageInfo);
            out(successInfo, socketId);

            //告诉其他客户端当前客户端上线
            String outS = "提示： " + userId + " 已上线";
            messageInfo.setMessageContent(outS);
            String infoToOthers = new Gson().toJson(messageInfo);
            outOthers(infoToOthers);
        }else{
            messageInfo.setMessageContent("用户 " + userId + " 已被使用，请更换用户名重新启动！");
            messageInfo.setAction(SocketUtil.ACTIONS[6]);
            String failInfo = new Gson().toJson(messageInfo);
            out(failInfo, socketId);
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
                RoomService roomService = new RoomService();
                ChatRoom room = roomService.createSingleChatRoom(messageInfo.getUserId(), messageInfo.getCheckUserId());
                roomId = room.getRoomId();
            }
            messageInfo.setRoomId(roomId);
        }
        out(new Gson().toJson(messageInfo), socketId);
    }

    //检查群聊聊天室是否存在且用户是否属于群聊成员
    public void checkRoom(MessageInfo messageInfo){
        RoomService roomService = new RoomService();
        ChatRoom room = roomService.getRoomByRoomId(messageInfo.getRoomId());
        if(room == null){
            messageInfo.setRoomId(null);
        }else {
            if (room.getRoomType().equals(ChatRoom.CHAT_TYPE_SINGLE)) {
                messageInfo.setRoomId(null);
            }else{
                RoomUserService roomUserService = new RoomUserService();
                List<String> userIds = roomUserService.getUserIdsByRoomId(room.getRoomId());
                if(!userIds.contains(messageInfo.getUserId())){
                    messageInfo.setRoomId(null);
                }
            }
        }
        out(new Gson().toJson(messageInfo), socketId);
    }

    public void showUnreadRoomHistory(MessageInfo messageInfo){
        String roomId = messageInfo.getRoomId();
        List<MessageInfo> messageInfos = new MessageInfoService().getMessageInfosByRoomId(roomId);
        if (messageInfos.size() > 0) {
            String historyStr = "";
            String messageMarkId = messageInfo.getMessageMarkId();
            String userId = messageInfo.getUserId();
            RoomUserService roomUserService = new RoomUserService();
            RoomUser roomUser = roomUserService.getRoomUserByRoomIdAndUserId(roomId,userId);
            int count = 0;
            for (int i = 0; i < messageInfos.size(); i++) {
                MessageInfo messageInfo1 = messageInfos.get(i);
                //未读标记之后的消息（不包括自己发送的消息且显示的消息都是自己加入聊天室之后的消息）
                if (Integer.parseInt(messageInfo1.getMessageId()) > Integer.parseInt(messageMarkId) && messageInfo1.getDate().after(roomUser.getCreateDate())) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    String dateStr = dateFormat.format(messageInfo1.getDate());
                    if(messageInfo1.getUserId() == null){
                        String messageStr = dateStr + ": " + "系统提示 " + messageInfo1.getMessageContent();
                        historyStr += messageStr + SocketUtil.LINE_SEPARATOR;
                        messageMarkId = messageInfo1.getMessageId();
                        messageInfo.setMessageMarkId(messageMarkId);
                        count ++;
                    }else if(!messageInfo1.getUserId().equals(userId)){
                        String messageStr = dateStr + ": " + messageInfo1.getUserId() + ": " + messageInfo1.getMessageContent();
                        historyStr += messageStr + SocketUtil.LINE_SEPARATOR;
                        messageMarkId = messageInfo1.getMessageId();
                        messageInfo.setMessageMarkId(messageMarkId);
                        count ++;
                    }

                }
            }
            if (count>0){
                historyStr += "------以上是未浏览消息记录("+ count +"条)------"+ SocketUtil.LINE_SEPARATOR;
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
                RoomService roomService = new RoomService();
                ChatRoom room =  roomService.getRoomByRoomId(roomId);
                String message = messageInfo.getMessageContent();
                for (int i = 0; i < userIds.size(); i++) {
                    if(!userIds.get(i).equals(messageInfo.getUserId())) {
                        String friendSocketId = SocketServer.userSocketMap.get(userIds.get(i));
                        Socket socket1 = SocketServer.socketMap.get(friendSocketId);
                        if (socket1 != null) {
                            String userId = SocketServer.socketUserMap.get(socketId);
                            messageInfo.setUserId(userId);
                            if(room.getRoomType().equals(ChatRoom.CHAT_TYPE_GROUP)){
                                messageInfo.setMessageContent("(聊天群 " + roomId + ")" + userId + ":" + message);
                            }else {
                                messageInfo.setMessageContent(userId + ":" + message);
                            }
                            //发送信息给目标客户端
                            out(new Gson().toJson(messageInfo), friendSocketId);
                        }
                    }
                }
            }
        }
    }

    //群通知
    public void outMessageToGroupRoom(String roomId,String message){
        if(roomId != null) {
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setRoomId(roomId);
            messageInfo.setAction(SocketUtil.ACTIONS[0]);
            messageInfo.setMessageContent(message);
            //将messageInfo存入本地文件
            messageInfo = new MessageInfoService().saveMessageInfo(messageInfo);
            RoomUserService roomUserService = new RoomUserService();
            List<String> userIds = roomUserService.getUserIdsByRoomId(roomId);
            if(userIds != null && userIds.size()>0){
                for (int i = 0; i < userIds.size(); i++) {
                    String friendSocketId = SocketServer.userSocketMap.get(userIds.get(i));
                    Socket socket1 = SocketServer.socketMap.get(friendSocketId);
                    if (socket1 != null) {
                        String userId = SocketServer.socketUserMap.get(socketId);
                        messageInfo.setUserId(userId);
                        messageInfo.setMessageContent(message);
                        //发送信息给目标客户端
                        out(new Gson().toJson(messageInfo), friendSocketId);
                    }
                }
            }
        }
    }

    public void outHistoryToClient(MessageInfo messageInfo){
        String historyStr = "";
        String roomId = messageInfo.getRoomId();
        //根据roomId获取消息记录
        List<MessageInfo> messageHistoryList = new MessageInfoService().getMessageInfosByRoomId(roomId);
        if(messageHistoryList.size()>0) {
            for (int i = 0; i < messageHistoryList.size(); i++) {
                MessageInfo sendHistory = messageHistoryList.get(i);
                if(sendHistory != null) {
                    String userId = messageInfo.getUserId();
                    RoomUserService roomUserService = new RoomUserService();
                    RoomUser roomUser = roomUserService.getRoomUserByRoomIdAndUserId(roomId,userId);
                    //用户只能查看自己加入之后的聊天信息
                    if(sendHistory.getDate().after(roomUser.getCreateDate())) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                        String dateStr = dateFormat.format(sendHistory.getDate());
                        String sendHistoryUserId = sendHistory.getUserId();
                        String messageStr = "";
                        if(!StringUtils.isEmpty(sendHistoryUserId)) {
                            if (sendHistoryUserId.equals(userId)) {
                                sendHistoryUserId = sendHistoryUserId + "(我)";
                            }
                            messageStr = dateStr + ": " + sendHistoryUserId + ": " + sendHistory.getMessageContent();
                        }else{
                            messageStr =dateStr + ": " + "系统提示 " + sendHistory.getMessageContent();
                        }
                        historyStr += messageStr + SocketUtil.LINE_SEPARATOR;
                        messageInfo.setMessageMarkId(sendHistory.getMessageId());
                    }
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

    public void createRoom(MessageInfo messageInfo){
        String message = "";
        String[] userIds = messageInfo.getUserIds();
        if(!ArrayUtils.isEmpty(userIds)){
            RoomService roomService = new RoomService();
            ChatRoom room = new ChatRoom();
            room.setRoomType(ChatRoom.CHAT_TYPE_GROUP);
            room = roomService.saveRoom(room);
            message += "聊天室创建成功!聊天室id为'"+ room.getRoomId() + "'! ";
            String roomId = room.getRoomId();
            RoomUser roomUser = new RoomUser();
            //将创建者加入聊天室
            roomUser.setUserId(messageInfo.getUserId());
            roomUser.setRoomId(roomId);
            RoomUserService roomUserService = new RoomUserService();
            roomUserService.saveRoomUser(roomUser);
            message += "当前用户" + messageInfo.getUserId() + "成功加入；";
            for (int i = 0; i < userIds.length; i++) {
                String userId = userIds[i];
                boolean userIsExist = new UserService().checkUserIsExist(userId);
                if(userIsExist){
                    if(!messageInfo.getUserId().equals(userId)){
                        roomUser = new RoomUser();
                        roomUser.setRoomId(roomId);
                        roomUser.setUserId(userId);
                        roomUserService.saveRoomUser(roomUser);
                        outMessageToGroupRoom(roomId,"------ " + userId + " 成功加入群聊 "+ roomId + " ------");
                    }
                }else{
                    message += "用户" + userId + "不存在!";
                }
            }
            messageInfo.setMessageContent(message);
            out(new Gson().toJson(messageInfo), socketId);
        }
    }

    public void viewRoomsList(MessageInfo messageInfo){
        String roomInfoStr = "";
        RoomUserService roomUserService = new RoomUserService();
        List<ChatRoom> chatRooms = roomUserService.getChatRomsByUserId(messageInfo.getUserId());
        if(chatRooms != null && chatRooms.size()>0)
        for (int i = 0; i < chatRooms.size(); i++) {
            ChatRoom room = chatRooms.get(i);
            if(room.getRoomType().equals(ChatRoom.CHAT_TYPE_GROUP)) {
                roomInfoStr += "roomId:" + room.getRoomId();
                List<String> userIds = roomUserService.getUserIdsByRoomId(room.getRoomId());
                if (userIds != null && userIds.size() > 0) {
                    roomInfoStr += "(";
                    for (int j = 0; j < userIds.size(); j++) {
                        if (j == userIds.size() - 1) {
                            roomInfoStr += userIds.get(j);
                        } else {
                            roomInfoStr += userIds.get(j) + ",";
                        }
                    }
                    roomInfoStr += ")" + SocketUtil.LINE_SEPARATOR;
                }
            }
        }
        if(StringUtils.isEmpty(roomInfoStr)){
            roomInfoStr += "#######您没有聊天群##########" + SocketUtil.LINE_SEPARATOR;
        }else{
            roomInfoStr += "#######以上是您的聊天群列表##########" + SocketUtil.LINE_SEPARATOR;
        }
        messageInfo.setMessageContent(roomInfoStr);
        out(new Gson().toJson(messageInfo), socketId);
    }

    public void addUsersToRoom(MessageInfo messageInfo){
        String message = "";
        String roomId = messageInfo.getRoomId();
        RoomService roomService = new RoomService();
        ChatRoom room = roomService.getRoomByRoomId(roomId);
        if(room != null && !room.getRoomType().equals(ChatRoom.CHAT_TYPE_SINGLE)) {
            RoomUserService roomUserService = new RoomUserService();
            List<String> userIdsIn = roomUserService.getUserIdsByRoomId(room.getRoomId());
            if(userIdsIn.contains(messageInfo.getUserId())) {
                String[] userIds = messageInfo.getUserIds();
                if (!ArrayUtils.isEmpty(userIds)) {
                    for (int i = 0; i < userIds.length; i++) {
                        String userId = userIds[i];
                        boolean userIsExist = new UserService().checkUserIsExist(userId);
                        if (userIsExist) {
                            RoomUser roomUser = roomUserService.getRoomUserByRoomIdAndUserId(roomId, userId);
                            if(roomUser != null){
                                message += "用户" + userId + "已在聊天群中；";
                            }else{
                                RoomUser roomUser1 = new RoomUser();
                                roomUser1.setRoomId(roomId);
                                roomUser1.setUserId(userId);
                                roomUserService.saveRoomUser(roomUser1);
                                outMessageToGroupRoom(roomId,"------ " + userId + " 成功加入群聊 "+ roomId + " ------");
                            }
                        } else {
                            message += "用户" + userId + "不存在!";
                        }
                    }
                }
            }else{
                message += "群聊聊天室不存在或当前用户不在此聊天群中";
            }
        }else{
            message += "群聊聊天室不存在或当前用户不在此聊天群中";
        }
        messageInfo.setMessageContent(message);
        out(new Gson().toJson(messageInfo), socketId);
    }

    //退出群聊
    public void leaveGroupRoom(MessageInfo messageInfo){
        String roomId = messageInfo.getRoomId();
        String userId = messageInfo.getUserId();
        RoomUserService roomUserService = new RoomUserService();
        roomUserService.deleteRoomUser(roomId, userId);
        messageInfo.setMessageContent("提示：已退出群聊 " + roomId);
        List<String> userIds = roomUserService.getUserIdsByRoomId(roomId);
        if(!(userIds != null && userIds.size()>0)){
            RoomService roomService = new RoomService();
            roomService.deleteRoom(roomId);
        }
        out(new Gson().toJson(messageInfo),socketId);
        outMessageToGroupRoom(roomId,"------ " + userId + " 退出了群聊 " + roomId + " ------");
    }
}

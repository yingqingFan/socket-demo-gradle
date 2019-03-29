package server;

import Entity.MessageInfo;
import client.ClientDemo;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerDemo {

    Map<String, Socket> socketMap = new HashMap<>();
    List<MessageInfo> messageHistoryList = new ArrayList<>();
    Gson gson = new Gson();
    String lineSeparator = System.getProperty("line.separator");

    public static void main(String[] args) throws IOException {
        ServerDemo serverDemo = new ServerDemo();
        //创建一个通信类的对象
        ServerSocket server = new ServerSocket(9999);
        //输出当前服务器的端口号
        System.out.println("服务器创建成功，端口号：" + server.getLocalPort());
        //容纳三个线程的线程池
        Executor pool = Executors.newFixedThreadPool(100);
        boolean flag = true;
        while (flag) {
            //阻塞等待客户端连接
            Socket socket = server.accept();
            String socketId = null;
            //为每个客户端分配唯一id
            while(StringUtils.isEmpty(socketId)||serverDemo.socketMap.get(socketId)!= null){
                socketId =((int) (Math.random()*100))+"";
            }
            //将socket存入内存中
            serverDemo.socketMap.put(socketId,socket);
            //new一个线程与客户端交互,server.accept()等待连接,pool执行线程
//            pool.execute(serverDemo.new ServerThread(socket, socketId));
            serverDemo.new ServerThread(socket, socketId+"").start();
        }
    }

    class ServerThread extends Thread {
        PrintWriter writer;//输出流
        BufferedReader bufferedReader;//输入流
        Socket socket;
        String socketId;

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

                //连接成功提示
                String successMessage =  "当前client" + socketId + " 连接成功";
                out(successMessage, socketId);

                //告诉其他客户端当前客户端上线
                String outS = "client" + socketId + " 已上线";
                outOthers(outS);

                //读取客户端信息并转发
                readAndSend();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //输出信息到指定客户端
        private void out(String outS, String socketId) {
            try {
                Socket socket1 = socketMap.get(socketId);
                if(socket1!=null) {
                    PrintWriter printWriter1 = new PrintWriter(socket1.getOutputStream());
                    //将信息发送客户机
                    printWriter1.println(outS);
                    //强制输出到命令行的界面中
                    printWriter1.flush();
                }else{
                    PrintWriter printWriter1 = new PrintWriter(socket.getOutputStream());
                    //提示客户端指定客户端不存在
                    printWriter1.println("指定client"+socketId+" 不存在,请退出重新选择！");
                    //强制输出到命令行的界面中
                    printWriter1.flush();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        //输出信息到其他所有客户端
        public void outOthers(String outS) {
            Iterator<String> idIterator = socketMap.keySet().iterator();
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
                        System.out.println("内容 : " + line);
                        MessageInfo messageInfo = gson.fromJson(line, MessageInfo.class);
                        //如果客户端是发送消息
                        if(messageInfo.getAction().equals(ClientDemo.ACTIONS[0])) {
                            messageInfo.setClientId(socketId);
                            String socketIdTo = messageInfo.getFriendClientId();
                            String message = messageInfo.getMessageContent();
                            //发送信息给目标客户端
                            out("client: " + socketId + " : " + message, socketIdTo);
                            //将messageInfo存入内存
                            messageHistoryList.add(messageInfo);
                        }
                        //如果客户端是查看聊天历史记录，返回历史记录给客户端
                        else if(messageInfo.getAction().equals(ClientDemo.ACTIONS[1])){
                            //将历史记录按时间排序
                            Collections.sort(messageHistoryList, new Comparator<MessageInfo>() {
                                @Override
                                public int compare(MessageInfo o1, MessageInfo o2) {
                                    if(o1.getDate().before(o2.getDate())) {
                                        return -1;
                                    }else{
                                        return 1;
                                    }
                                }
                            });
                            String historyStr="";
                            for(int i = 0; i < messageHistoryList.size(); i++){
                                MessageInfo sendHistory = messageHistoryList.get(i);
                                if((sendHistory.getClientId().equals(socketId) && sendHistory.getFriendClientId().equals(messageInfo.getFriendClientId()))
                                        || (sendHistory.getClientId().equals(messageInfo.getFriendClientId()) && sendHistory.getFriendClientId().equals(socketId)) ) {
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS a");
                                    String dateStr = dateFormat.format(sendHistory.getDate());
                                    String messageStr = dateStr + ":clientId" + sendHistory.getClientId() + ":" + sendHistory.getMessageContent();
                                    if(i == messageHistoryList.size()-1) {
                                        historyStr += messageStr;
                                    }else{
                                        historyStr += messageStr + lineSeparator;
                                    }
                                }
                            }
                            //输出历史到客户端
                            out(historyStr, socketId);
                        }
                        //如果客户端是查看在线用户，返回在线用户给客户端
                        else if(messageInfo.getAction().equals(ClientDemo.ACTIONS[2])){
                            Iterator<String> idIterator = socketMap.keySet().iterator();
                            while(idIterator.hasNext()){
                                String sId = idIterator.next();
                                out(sId, socketId);
                            }
                        }
                    }
                }
            }catch (IOException e) {
//                e.printStackTrace();
                socketMap.remove(socketId);
                String outS = "client" + socketId + " 已下线";
                System.out.println("client" + socketId + " 断开连接");
                outOthers(outS);
            }
        }
    }
}

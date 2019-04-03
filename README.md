#功能

##1.登录：
* 客户端与服务器连接，返回唯一标识与用户信息绑定

##2.聊天（一对多）： 
* 查看在线用户；  
* 上线通知，下线通知；   
* 聊天时存储相应聊天信息；
* 客户端重新连接；  
* 会话：初始时接收所有消息，与目标用户聊天时先显示与目标用户间的聊天记录，只打印与目标用户聊天的消息，其它用户发送的消息暂不打印，存储起来；  

##3.历史：
* 查看与相应用户之间的聊天信息

#运行
* 将client和server打成jar；  
* 运行命令 java -jar server.jar [参数：历史数据存储目录], java -jar client.jar [参数：用户名]
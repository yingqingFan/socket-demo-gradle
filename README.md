#功能

##1.登录：
* 客户端与服务器连接，服务端生成唯一标识与用户信息绑定

##2.聊天（一对多）： 
* 查看在线用户；  
* 上线通知，下线通知；   
* 聊天时先展示未读消息(进入聊天前接收所有用户消息，接收到的消息算作未读消息，进入聊天时会展示出来)，只打印与目标用户聊天的消息，暂不显示其他用户发送的消息，所有聊天信息都会存储起来；
* 客户端重新连接；

##3.历史：
* 查看与相应用户之间的聊天信息

#运行
* 运行命令：gradle build，生成 server-1.0.jar 和 client-1.0.jar
* 运行命令：java -jar server-1.0.jar [参数：数据存储目录], java -jar client-1.0.jar [参数：用户名] [参数：数据存储目录]
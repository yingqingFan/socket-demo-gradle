package com.ls.socket.service;

import com.ls.socket.entity.User;
import com.ls.socket.util.DataUtil;

import java.util.List;

public class UserService {

    public static String USER_FILE_PATH = null;

    public boolean checkUserIsExist(String userId){
        User user = getUserByUserId(userId);
        if(user == null){
            return false;
        }else{
            return true;
        }
    }

    public User getUserByUserId(String userId){
        User result = null;
        List<User> users = new DataUtil<User>().readFromFile(USER_FILE_PATH,User.class);
        for (int i = 0; i < users.size(); i++) {
            User user  = users.get(i);
            if(user.getUserId().equals(userId)){
                result = user;
            }
        }
        return result;
    }

    public void saveUser(User user){
        new DataUtil<User>().writeToFile(USER_FILE_PATH, user);
    }
}

package com.ls.socket.util;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataUtil<T> {
    private static Logger logger = Logger.getLogger(DataUtil.class);
    public void writeToFile(String path, T object){
        File file = new File(path);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            Gson gson = new Gson();
            String data = gson.toJson(object);
            try(FileWriter fileWriter = new FileWriter(file, true)){
                fileWriter.write(data + SocketUtil.LINE_SEPARATOR);
                fileWriter.flush();
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public  List<T> readFromFile(String path, Class<T> type){
        List<T> list = new ArrayList<T>();
        File file = new File(path);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            try(FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                Gson gson = new Gson();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    T object = gson.fromJson(line, type);
                    list.add(object);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return list;
    }
}

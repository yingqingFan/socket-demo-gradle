package com.ls.socket.util;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataUtil<T> {
    public void writeToFile(String path, T object){
        File file = new File(path);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            Gson gson = new Gson();
            String data = gson.toJson(object);
            String lineSeparator = System.getProperty("line.separator");
            fileWriter.write(data + lineSeparator);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  List<T> readFromFile(String path, Class<T> type){
        List<T> list = new ArrayList<T>();
        File file = new File(path);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            Gson gson = new Gson();
            String line = null;
            while((line = bufferedReader.readLine()) != null){
                T object = gson.fromJson(line,type);
                list.add(object);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}

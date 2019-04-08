package com.ls.socket.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class FileUtil {
    private static Logger logger = Logger.getLogger(FileUtil.class);
    public static File createFileIfNotExist(String path){
        File file = new File(path);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error("File create error!", e);
            }
        }
        return file;
    }
}

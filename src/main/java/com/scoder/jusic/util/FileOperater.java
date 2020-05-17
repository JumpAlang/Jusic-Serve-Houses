package com.scoder.jusic.util;

/**
 * @author alang
 * @create 2020-01-12 15:35
 */

import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class FileOperater {


    public static String getfileinfo(String tokenPath) {
        String rstr = "";

        try {
            FileSystemResource resource = new FileSystemResource(tokenPath);
            BufferedReader br = new BufferedReader(new FileReader(resource.getFile()));
            String str = null;
            while ((str = br.readLine()) != null) {
                rstr += str;
            }
            br.close();
        } catch (IOException e) {
            //todo loginfo
        }
        return rstr;
    }

    public static void writefileinfo(String t,String tokenPath) throws IOException {
        FileSystemResource resource = new FileSystemResource(tokenPath);
            FileWriter fileWriter = (new FileWriter(resource.getFile()));
            fileWriter.write(t);
            fileWriter.close();
    }
}
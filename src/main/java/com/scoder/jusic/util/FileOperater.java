package com.scoder.jusic.util;

/**
 * @author alang
 * @create 2020-01-12 15:35
 */

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.*;


public class FileOperater {


    public static String getfileinfoByClassPath(String classPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classPath);
        String content = null;
        content = commonReadFile(resource);

        return content;
    }

    public static String commonReadFile(File file) throws IOException {
        String fileStr = "";
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str = "";
            while ((str = br.readLine()) != null) {
                fileStr += str;
            }
            br.close();
        return fileStr;
    }

    public static String commonReadFile(Resource resource) throws IOException {
        String fileStr = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String str = "";
            while ((str = br.readLine()) != null) {
                fileStr += str;
            }
            br.close();
        return fileStr;
    }

    public static String getfileinfo(String tokenPath) throws IOException {
        FileSystemResource resource = new FileSystemResource(tokenPath);
        return commonReadFile(resource.getFile());
    }

    public static void writefileinfo(String t,String tokenPath) throws IOException {
        FileSystemResource resource = new FileSystemResource(tokenPath);
            FileWriter fileWriter = (new FileWriter(resource.getFile()));
            fileWriter.write(t);
            fileWriter.close();
    }

    public static void writefileinfo(String t,Resource resource) throws IOException {
        FileWriter fileWriter = (new FileWriter(resource.getFile()));
        fileWriter.write(t);
        fileWriter.close();
    }
}
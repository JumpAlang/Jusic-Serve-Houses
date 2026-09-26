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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return readAllLines(reader);
        }
    }

    public static String commonReadFile(Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            return readAllLines(reader);
        }
    }

    public static String getfileinfo(String tokenPath) throws IOException {
        FileSystemResource resource = new FileSystemResource(tokenPath);
        return commonReadFile(resource.getFile());
    }

    public static void writefileinfo(String t,String tokenPath) throws IOException {
        FileSystemResource resource = new FileSystemResource(tokenPath);
        try (FileWriter fileWriter = new FileWriter(resource.getFile())) {
            fileWriter.write(t);
        }
    }

    public static void writefileinfo(String t,Resource resource) throws IOException {
        try (FileWriter fileWriter = new FileWriter(resource.getFile())) {
            fileWriter.write(t);
        }
    }

    private static String readAllLines(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        return content.toString();
    }
}

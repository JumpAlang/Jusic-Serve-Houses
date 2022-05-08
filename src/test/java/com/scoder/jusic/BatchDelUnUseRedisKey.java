package com.scoder.jusic;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JumpAlang
 * @create 2022-01-03 16:44
 */
public class BatchDelUnUseRedisKey {

    @Test
    public List<String> getHouseList() throws UnirestException {
        List<String> currentKeys = new ArrayList();
        currentKeys.add("jusic_default_8888__");
        currentKeys.add("jusic_config_8888_DEFAULT");
        currentKeys.add("jusic_houses_8888_");
        HttpResponse<String> response = Unirest.post("https://tx.alang.run/api/house/search").asString();
        JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
        if (responseJsonObject.getInteger("code").equals(20000)) {
            JSONArray data = responseJsonObject.getJSONArray("data");
            for(int i = 0; i < data.size(); i++){
                String id = data.getJSONObject(i).getString("id");
                currentKeys.add("jusic_session_8888_"+id);
                currentKeys.add("jusic_pick_8888_"+id);
                currentKeys.add("jusic_default_8888__"+id);
                currentKeys.add("jusic_config_8888_"+id);
                currentKeys.add("jusic_playing_8888_"+id);
            }

            System.out.println(currentKeys);
        }
        if(currentKeys.size() <= 3){
            throw new RuntimeException("查询实时在线房间数有异常");
        }
        return currentKeys;
    }

    @Test
    public void batchDel() throws IOException, UnirestException {
        FileReader fileReader = new FileReader("D:\\java\\workspace\\Jusic-Serve-Houses\\src\\test\\resources\\allRedisKey.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String key;
        String willRemoveKeyPath = "D:\\java\\workspace\\Jusic-Serve-Houses\\src\\test\\resources\\";
        int count = 0;
        StringBuilder sb = new StringBuilder("");
        List<String> list = getHouseList();
        while((key=bufferedReader.readLine()) != null){
            if(!list.contains(key)){
                sb.append(key + "\n");
                if((++count) % 30000 == 0){
                    FileWriter fileWriter = new FileWriter(willRemoveKeyPath+count+".txt");
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(sb.toString());
                    bufferedWriter.close();
                    sb = new StringBuilder("");
                }
            }
        }
        if(sb.toString().length() > 0){
//        if(sb.toString().startsWith("del") && sb.toString().length() > 3){
            FileWriter fileWriter = new FileWriter(willRemoveKeyPath+count+".txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(sb.toString());
            bufferedWriter.close();
        }
        bufferedReader.close();
    }
}

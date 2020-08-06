package com.scoder.jusic.job;

/**
 * @author alang
 * @create 2020-01-12 14:50
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.util.FileOperater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class MusicTopJob {
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private  ResourceLoader resourceLoader;

//    public static void main(String[] args) {
//        getData(topUrl);
//    }

    //每天0时更新
    @Scheduled(cron = "0 0 0 * * ? ")//fixedRate = 10800000)//表示每隔3小时
    public void getMusicTopJob(){
        JusicProperties.setDefaultListByJob(getData());
    }

    public ArrayList<String> getMusicTop(){
        return getData();
    }

    private class TopMusic{
        private ArrayList<String> topMusicList;
        private String topMusicStrings;

        public ArrayList<String> getTopMusicList() {
            return topMusicList;
        }

        public void setTopMusicList(ArrayList<String> topMusicList) {
            this.topMusicList = topMusicList;
        }

        public String getTopMusicStrings() {
            return topMusicStrings;
        }

        public void setTopMusicStrings(String topMusicStrings) {
            this.topMusicStrings = topMusicStrings;
        }

        public TopMusic() {
        }

        public TopMusic(ArrayList<String> topMusicList, String topMusicStrings) {
            this.topMusicList = topMusicList;
            this.topMusicStrings = topMusicStrings;
        }
    }

    private TopMusic getTopMusicWy(){
//        ArrayList<String> topList = new ArrayList<>();
//        String musicIds = "";
//        Document doc = null;
//        try {
//            doc = Jsoup.connect(jusicProperties.getWyTopUrl()).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
//                    .header("Accept-Encoding", "gzip, deflate, br")
//                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
//                    .header("Cookie", "ntes_nuid=808c5b4190e482c6c67db2d7227d2385; _ga=GA1.2.1721653250.1519607055; _iuqxldmzr_=32; __gads=ID=5722bf9696c74e28:T=1521102195:S=ALNI_MZYcpZxkIf_16lrYuHaFilh1muDuA; vjuids=2419c60fa.16228c19127.0.880b622cc329f; vjlast=1521102197.1522307712.11; mail_psc_fingerprint=90d932af4be39cb7fa8096d9fbc6d993; WM_TID=FD2f5aM9WfS3MJRugepLcrsvoiEopav5; __utma=94650624.1721653250.1519607055.1532763446.1541508445.7; _ntes_nnid=808c5b4190e482c6c67db2d7227d2385,1553134932734; ntes_kaola_ad=1; NTES_CMT_USER_INFO=274741771%7C%E6%9C%89%E6%80%81%E5%BA%A6%E7%BD%91%E5%8F%8B0go3Eb%7Chttp%3A%2F%2Fcms-bucket.nosdn.127.net%2F2018%2F08%2F13%2F078ea9f65d954410b62a52ac773875a1.jpeg%7Cfalse%7CYTk4d2VpYm9AMTYzLmNvbQ%3D%3D; vinfo_n_f_l_n3=233d52faaa62a9ad.1.7.1521102197055.1562858403508.1577250207706; nts_mail_user=a03chu@163.com:-1:1; MUSIC_U=aa205eed7a23c9d4518904f42fbd98108baae9eb913a6dd3692b8e83818c0b3592a0692d5fa1c37d6e15bb824a3909ee679ca02a3541161a37c7b5136c1b5ca0774bd83c58ab20097955a739ab43dce1; __remember_me=true; __csrf=64fd8b3e46e8c3706d3cdb1ad8a246c0; P_INFO=\"a08xiaozhi@163.com|1578627032|0|unireg|00&99|null&null&null#fuj&350500#10#0#0|&0||a08xiaozhi@163.com\"; JSESSIONID-WYYY=Q1xxHvA77Q7f8EPF8VGwoN44uzox0OuZ%5CHRsJe%2B%5C%2FGZo7MVSbMTGFumtthiQ7Tp%5Cbays%5CEx7PS%5C9b6e13hK0WyY%5CAGz27Q%2BX4CkcoD7yWQPMzxFgNsrMlkS3HYfgIUDX33VFccc%2F%2FZ%5CvOq%2F5hWY2bVWCPJiK5CI3D%5C%5CF%5Cr0a8VobKfJF%3A1578813326264; WM_NI=B6GLAY8juHRgTaF4%2BS8HD0z7Om5sP5Jr31CIdMha3dJze942eru1BRQrcBNnk4OO9kagV37HND1rR0%2BaBdjpaTK35%2BIiK2j10OfrThpnmh8kNbGDwTr1k1UuupUoCGtgdmw%3D; WM_NIKE=9ca17ae2e6ffcda170e2e6ee8bcc5982f1b6b8f36393ef8ea3d54a968e8abab821b8be8f8cc14ea88e9992f92af0fea7c3b92a82ea89adc453fcb6bba9d16d98bf8ad5aa59fc988cd1f93d8db3fb94d33f8aaefdd2c243a893a882b2708aa6abdab73aa7aebbd2c15a93aeada3cd66a9ec81b0bc61a3b99ad4b6448e9cff93d13ce9bf8e85e97ca38c85d4b8218bb785d3e93bf491aa93d764b4b2a389bb618ebe89a5f660b2efa5d2d87fb7b09b90c24db8b9ac8ef637e2a3")
//                    .header("Referer", "https://music.163.com")
//                    .header("Upgrade-Insecure-Requests", "1")
//                    .method(Connection.Method.GET)
//                    .timeout(200000).get(); // 设置请求头等信息，模拟人工访问，超时时间可自行设置
//
//            Elements names = doc.select("#song-list-pre-cache a");
//
//            String musicId = "";
//            for (Element element : names) {
//                musicId = element.attr("href").
//                        replace("/song?id=", "").trim();
//                if (musicId != null && musicId != "") {
//                    musicIds += musicId+"\n";
//                    topList.add(musicId);
//                }
//            }
//        } catch (Exception e) {
//            log.error("mg音乐热门歌曲获取失败; Exception: [{}]",e.getMessage());
//        }
//        return new TopMusic(topList,musicIds);
        HttpResponse<String> response = null;
        String musicIds = "";
        ArrayList<String> topList = new ArrayList<>();
        Integer failCount = 0;

        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/playlist/detail?id=")
                .append(jusicProperties.getWyTopUrl());
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(url.toString())
                        .asString();
                JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
                if (responseJsonObject.getInteger("code") != 200) {
                    failCount++;
                } else {
                    JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                    int size = data.size();
                    String musicId = "";
                    for(int i = 0; i < size; i++) {
                            musicId = data.getJSONObject(i).getString("id");
                            if (musicId != null && musicId != "") {
                                musicIds += musicId+"\n";
                                topList.add(musicId);
                            }
                    }
                    break;
                }
            } catch (Exception e) {
                failCount++;
                log.error("网易音乐热门歌曲获取失败; Exception: [{}]",e.getMessage());
            }
        }

        return new TopMusic(topList,musicIds);
    }
    private TopMusic getTopMusicQQ() {
        HttpResponse<String> response = null;
        String musicIds = "";
        ArrayList<String> topList = new ArrayList<>();
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/top?id=26&pageSize=300")
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        JSONArray data = jsonObject.getJSONObject("data").getJSONArray("list");
                        int size = data.size();
                        String musicId = "";
                        for(int i = 0; i < size; i++) {
                            musicId = data.getJSONObject(i).getString("mid");
                            if (musicId != null && musicId != "") {
                                musicIds += musicId+"___qq" + "\n";
                                topList.add(musicId+"___qq");
                            }
                        }
                        break;
                    }else{
                        return new TopMusic(topList,"");
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐热门歌曲获取失败; Exception: [{}]",e.getMessage());
            }
        }

        return new TopMusic(topList,musicIds);
    }
    public ArrayList<String> getData() {
        TopMusic topMusicWy = getTopMusicWy();
        TopMusic topMusicQq = getTopMusicQQ();
        String allMusicIdsStr = "";
        ArrayList<String> allMusicIdsList = new ArrayList<>();
        if (topMusicWy.getTopMusicStrings() != "") {
            allMusicIdsStr += topMusicWy.getTopMusicStrings();
            allMusicIdsList.addAll(topMusicWy.getTopMusicList());
        }
        if(topMusicQq.getTopMusicStrings() != ""){
            allMusicIdsStr += topMusicQq.getTopMusicStrings();
            allMusicIdsList.addAll(topMusicQq.getTopMusicList());
        }
        if(allMusicIdsStr != ""){
            try {
                FileOperater.writefileinfo(allMusicIdsStr, resourceLoader.getResource(jusicProperties.getDefaultMusicFile()));
            } catch (IOException e) {
                log.error("写入热门歌曲id失败，IOException:[{}]",e.getMessage());
            }
        }
        return allMusicIdsList;
    }
}
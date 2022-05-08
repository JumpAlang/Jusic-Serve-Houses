package com.scoder.jusic;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.model.Music;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alang
 * @create 2020-05-11 0:22
 */
public class TestQQMusic {
    public static void main(String[] args) throws UnsupportedEncodingException {
//        search("周杰伦");
//        searchQQ("周杰伦");
//        1436709403
//        getMusic("1436709403");
//        getQQMusicUrl("0039MnYb0qxYhV");
//        setCookie();;
        String a="\\xac\\xed\\x00\\x05t\\x00\\x1b";//"\\xe4\\xb8\\x80\\xe8\\xb5\\xb7\\xe5\\x90\\xac\\xe6\\xad\\x8c\\xe5\\x90\\xa7";
        String ab = a.replaceAll("\\\\x", "%");
        System.out.println(URLDecoder.decode(ab, "UTF-8"));
        String c = "love story";
        System.out.println(URLEncoder.encode(c,"UTF-8"));
        testKwXm();
    }

    public static void testKwXm(){
        HttpResponse<String> response = null;
        try {
            response = Unirest.get("http://120.24.243.237:8081" + "/api/searchsingle").queryString("provider","kuwo").queryString("keyword","周杰伦+Mojito").asString();

            if (response.getStatus() != 200) {
            } else {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getString("code").equals("20000")) {
                    String result = jsonObject.getString("data");
                    System.out.println(result);
                }
            }
        } catch (Exception e) {
        }

    }

    public static void search(String keyWord) {
        StringBuilder url = new StringBuilder()
                .append("http://120.24.243.237:8888")
                .append("/netease/songs/")
                .append(keyWord)
                .append("/search")
                .append("/").append(0)
                .append("/").append(20);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 1) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("data");
                Integer count = responseJsonObject.getJSONObject("data").getInteger("count");
                List list = JSONObject.parseObject(JSONObject.toJSONString(data), List.class);
                System.out.println("count:"+count);
                System.out.println("list:"+list);
            } else {
            }
        } catch (UnirestException e) {
        }
    }


    public static void searchQQ(String keyWord) {
        StringBuilder url = new StringBuilder()
                .append("http://120.24.243.237:3300/search?key=")
                .append(keyWord)
                .append("&pageNo=").append(1)
                .append("&pageSize=").append(20);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albummid");
                    buildJSONObject.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    String singerName = singerArray.getJSONObject(0).getString("name");
                    buildJSONObject.put("artist",singerName);
                    String songname = jsonObject.getString("songname");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("songmid");
                    buildJSONObject.put("id","qq_"+songmid);

                    JSONObject privelege = new JSONObject();
                    privelege.put("st",1);
                    privelege.put("fl",1);
                    buildJSONObject.put("privilege",privelege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    String albumid = jsonObject.getString("albumid");
                    String albumname = jsonObject.getString("albumname");
                    album.put("id","qq_"+albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                System.out.println("count:"+count);
                System.out.println("list:"+list);
            } else {
            }
        } catch (UnirestException e) {
        }
    }

    public static ArrayList<String> getData(String url) {
        ArrayList<String> topList = new ArrayList<>();
        Document doc = null;
        try {
            doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Cookie", "ntes_nuid=808c5b4190e482c6c67db2d7227d2385; _ga=GA1.2.1721653250.1519607055; _iuqxldmzr_=32; __gads=ID=5722bf9696c74e28:T=1521102195:S=ALNI_MZYcpZxkIf_16lrYuHaFilh1muDuA; vjuids=2419c60fa.16228c19127.0.880b622cc329f; vjlast=1521102197.1522307712.11; mail_psc_fingerprint=90d932af4be39cb7fa8096d9fbc6d993; WM_TID=FD2f5aM9WfS3MJRugepLcrsvoiEopav5; __utma=94650624.1721653250.1519607055.1532763446.1541508445.7; _ntes_nnid=808c5b4190e482c6c67db2d7227d2385,1553134932734; ntes_kaola_ad=1; NTES_CMT_USER_INFO=274741771%7C%E6%9C%89%E6%80%81%E5%BA%A6%E7%BD%91%E5%8F%8B0go3Eb%7Chttp%3A%2F%2Fcms-bucket.nosdn.127.net%2F2018%2F08%2F13%2F078ea9f65d954410b62a52ac773875a1.jpeg%7Cfalse%7CYTk4d2VpYm9AMTYzLmNvbQ%3D%3D; vinfo_n_f_l_n3=233d52faaa62a9ad.1.7.1521102197055.1562858403508.1577250207706; nts_mail_user=a03chu@163.com:-1:1; MUSIC_U=aa205eed7a23c9d4518904f42fbd98108baae9eb913a6dd3692b8e83818c0b3592a0692d5fa1c37d6e15bb824a3909ee679ca02a3541161a37c7b5136c1b5ca0774bd83c58ab20097955a739ab43dce1; __remember_me=true; __csrf=64fd8b3e46e8c3706d3cdb1ad8a246c0; P_INFO=\"a08xiaozhi@163.com|1578627032|0|unireg|00&99|null&null&null#fuj&350500#10#0#0|&0||a08xiaozhi@163.com\"; JSESSIONID-WYYY=Q1xxHvA77Q7f8EPF8VGwoN44uzox0OuZ%5CHRsJe%2B%5C%2FGZo7MVSbMTGFumtthiQ7Tp%5Cbays%5CEx7PS%5C9b6e13hK0WyY%5CAGz27Q%2BX4CkcoD7yWQPMzxFgNsrMlkS3HYfgIUDX33VFccc%2F%2FZ%5CvOq%2F5hWY2bVWCPJiK5CI3D%5C%5CF%5Cr0a8VobKfJF%3A1578813326264; WM_NI=B6GLAY8juHRgTaF4%2BS8HD0z7Om5sP5Jr31CIdMha3dJze942eru1BRQrcBNnk4OO9kagV37HND1rR0%2BaBdjpaTK35%2BIiK2j10OfrThpnmh8kNbGDwTr1k1UuupUoCGtgdmw%3D; WM_NIKE=9ca17ae2e6ffcda170e2e6ee8bcc5982f1b6b8f36393ef8ea3d54a968e8abab821b8be8f8cc14ea88e9992f92af0fea7c3b92a82ea89adc453fcb6bba9d16d98bf8ad5aa59fc988cd1f93d8db3fb94d33f8aaefdd2c243a893a882b2708aa6abdab73aa7aebbd2c15a93aeada3cd66a9ec81b0bc61a3b99ad4b6448e9cff93d13ce9bf8e85e97ca38c85d4b8218bb785d3e93bf491aa93d764b4b2a389bb618ebe89a5f660b2efa5d2d87fb7b09b90c24db8b9ac8ef637e2a3")
                    .header("Referer", "https://music.163.com")
                    .header("Upgrade-Insecure-Requests", "1")
                    .method(Connection.Method.GET)
                    .timeout(200000).get(); // 设置请求头等信息，模拟人工访问，超时时间可自行设置

        } catch (IOException e) {
            e.printStackTrace();
        }
        return topList;
    }


    public static Music getMusic(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < 3) {
            try {
                response = Unirest.get("http://120.24.243.237:8888" + "/netease/song/" + keyword)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("code").equals(1)) {
                        music = JSONObject.parseObject(jsonObject.get("data").toString(), Music.class);
                        System.out.println(music);
                        break;
                    }
                }
            } catch (UnirestException e) {
                failCount++;
            }
        }

        return music;
    }

    public static String getQQMusicUrl(String musicId) {
        HttpResponse<String> response = null;
        String result = null;

        Integer failCount = 0;

        while (failCount < 2) {
            try {
                response = Unirest.get("http://120.24.243.237:3300" + "/song/urls?id="+musicId)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString(musicId);
                        break;
                    }
                }
            } catch (UnirestException e) {
                failCount++;
            }
        }

        return result;
    }

    public static void setCookie() {
        HttpResponse<String> response = null;
        String result = null;

        Integer failCount = 0;

        while (failCount < 2) {
            try {
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("data","pgv_pvi=8420090880; RK=4SoN/bplUE; ptcz=5c2a65aa03241de7cf9f359168280f4bbbb69bf4f8c94491342eee656300ad5c; pgv_pvid=4980542695; pac_uid=1_1040927107; tvfe_boss_uuid=f7b09f131231465c; o_cookie=1040927107; ts_uid=6085208916; ts_refer=www.baidu.com/link; ied_qq=o1040927107; XWINDEXGREY=0; pgv_info=ssid=s5666089788; pgv_si=s4261020672; _qpsvr_localtk=0.3445361468266863; qqmusic_fromtag=66; userAction=…");
                response = Unirest.post("http://localhost:3300" + "/user/setCookie").queryString("data", "pgv_pvi=8420090880; RK=4SoN/bplUE; ptcz=5c2a65aa03241de7cf9f359168280f4bbbb69bf4f8c94491342eee656300ad5c; pgv_pvid=4980542695; pac_uid=1_1040927107; tvfe_boss_uuid=f7b09f131231465c; o_cookie=1040927107; ts_uid=6085208916; ts_refer=www.baidu.com/link; ied_qq=o1040927107; XWINDEXGREY=0; pgv_info=ssid=s5666089788; pgv_si=s4261020672; _qpsvr_localtk=0.3445361468266863; qqmusic_fromtag=66; userAction=1; yq_playschange=0; yq_playdata=; player_exist=1; yq_index=0; yplayer_open=0; yqq_stat=0; ptui_loginuin=525158920345%20; psrf_qqrefresh_token=98676A02938515711F676471410AAD16; psrf_qqaccess_token=183F1991223C89A0DC32BD3ADC6C88E2; qqmusic_key=Q_H_L_2WYKvw50eO4UofFA705_QIFQg-pIlTgW2F1SLR8xq_pFSwEbi7kU7RlKJRePsR0; uin=1040927107; psrf_qqopenid=53BFFA7D085BCF721A880591E9E41480; psrf_musickey_createtime=1589203496; qm_keyst=Q_H_L_2WYKvw50eO4UofFA705_QIFQg-pIlTgW2F1SLR8xq_pFSwEbi7kU7RlKJRePsR0; psrf_qqunionid=07616D6D42A255DC81D807ADAF228F89; psrf_access_token_expiresAt=1596979495; ts_last=y.qq.com/portal/search.html")
                  .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        break;
                    }
                }
            } catch (UnirestException e) {
                failCount++;
            }
        }

    }
}

package com.scoder.jusic.util;

import com.alibaba.fastjson.JSONObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.nio.charset.StandardCharsets;

/**
 * @author JumpAlang
 * @create 2023-07-18 19:40
 */
// 小Q
public class QQTrackUrlReq {
    /**
     * 获取 QQ 音乐歌曲链接
     *
     * @param mid     歌曲 id
     * @param quality 品质(sq hr hq mp3)
     * @return
     */
    public String getTrackUrl(String mid, String quality) throws Exception {
        String platform = "qq";
        String device = "MI 14 Pro Max";
        String osVersion = "27";
        long time = System.currentTimeMillis() / 1000;
        String lowerCase = StringUtil.toMD5("f389249d91bd845c9b817db984054cfb" + time + "6562653262383463363633646364306534333663").toLowerCase();

        String s6 = "{\\\"method\\\":\\\"GetMusicUrl\\\",\\\"platform\\\":\\\"" + platform + "\\\",\\\"t1\\\":\\\"" + mid + "\\\",\\\"t2\\\":\\\"" + quality + "\\\"}";
        String s7 = "{\\\"uid\\\":\\\"\\\",\\\"token\\\":\\\"\\\",\\\"deviceid\\\":\\\"84ac82836212e869dbeea73f09ebe52b\\\",\\\"appVersion\\\":\\\"4.1.0.V4\\\"," +
                "\\\"vercode\\\":\\\"4100\\\",\\\"device\\\":\\\"" + device + "\\\",\\\"osVersion\\\":\\\"" + osVersion + "\\\"}";
        String s8 = "{\n\t\"text_1\":\t\"" + s6 + "\",\n\t\"text_2\":\t\"" + s7 + "\",\n\t\"sign_1\":\t\"" + lowerCase + "\",\n\t\"time\":\t\""
                + time + "\",\n\t\"sign_2\":\t\"" + StringUtil.toMD5(s6.replace("\\", "") + s7.replace("\\", "")
                + lowerCase + time + "NDRjZGIzNzliNzEx").toLowerCase() + "\"\n}";

        byte[] aesBytes = CryptoUtil2.encrypt(s8.getBytes(), "6480fedae539deb2".getBytes(),CryptoUtil2.ALGORITHM_AES);
        s8 = CryptoUtil2.byte2hex(aesBytes);
        byte[] encodedBytes = CryptoUtil2.byte2hex(s8.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        byte[] compressedBytes = CryptoUtil2.compress(encodedBytes);
        String[] urls = {
//                "http://app.kzti.top:1030/client/cgi-bin/api.fcg",
//                "http://119.91.134.171:1030/client/cgi-bin/api.fcg",
                "http://106.52.68.150:1030/client/cgi-bin/api.fcg"
        };
        String url = urls[0];

        try {
            HttpResponse<byte[]> resp = Unirest.post(url).body(compressedBytes).asBytes();
            byte[] decompressed = CryptoUtil2.decompress(resp.getBody(),0,resp.getBody().length);
            String body = new String(decompressed, StandardCharsets.UTF_8);
            String trackUrl = JSONObject.parseObject(body).getString("data");
            return trackUrl;
        } catch (Exception e) {
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        QQTrackUrlReq qqTrackUrlReq = new QQTrackUrlReq();
        String url = qqTrackUrlReq.getTrackUrl("0039MnYb0qxYhV","320k");
        System.out.println(url);
    }
}
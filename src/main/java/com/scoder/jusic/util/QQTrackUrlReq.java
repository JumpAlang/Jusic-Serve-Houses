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
    public String getTrackUrl(String mid, String quality) {
      return null;
    }

    public static void main(String[] args) throws Exception {
        QQTrackUrlReq qqTrackUrlReq = new QQTrackUrlReq();
        String url = qqTrackUrlReq.getTrackUrl("002eB80M3JAI55","320k");//0039MnYb0qxYhV
        System.out.println(url);
    }
}
package com.scoder.jusic;

import kong.unirest.Unirest;

/**
 * @author JumpAlang
 * @create 2020-08-07 16:23
 */
public class TestRandom {
    public static void main(String[] args) {
        System.out.println(Unirest.get("https://alang-srt.oss-cn-beijing.aliyuncs.com/lizhimusic.json").asString().getBody());
    }
}

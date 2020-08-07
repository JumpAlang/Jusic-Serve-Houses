package com.scoder.jusic.model;

import lombok.*;

import java.io.Serializable;
import java.util.HashSet;

/**
 * @author H
 */
@EqualsAndHashCode
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Music extends Message implements Serializable {

    private static final long serialVersionUID = -5505741219684417455L;

    /**
     * 音乐 id
     */
    @EqualsAndHashCode.Include
    private String id;
    /**
     * 音乐名
     */
    @EqualsAndHashCode.Include
    private String name;
    /**
     * 歌手
     */
    private String artist;
    /**
     * 时长
     */
    private Long duration;
    /**
     * 音乐链接
     */
    private String url;
    /**
     * 歌词
     */
    private String lyric;
    /**
     * 专辑图片
     */
    private String pictureUrl;
    /**
     * 专辑信息
     */
    private Album album;
    /**
     * 选歌时间，毫秒时间戳
     */
    @EqualsAndHashCode.Include
    private long pickTime;
    /**
     * 推送时间
     */
    private Long pushTime;

    private HashSet<String> ips = new HashSet<>();

    private Long goodTime;

    private Long topTime;

    private String source = "wy";//搜索来源：网易、qq

}

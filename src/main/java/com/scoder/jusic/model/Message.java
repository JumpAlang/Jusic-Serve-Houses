package com.scoder.jusic.model;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author H
 */
@Data
@ToString
public class Message implements Serializable {

    private static final long serialVersionUID = -5505741219675317455L;

    private String sessionId;
    /**
     * 内容
     */
    private String content;
    /**
     * 昵称，消息发送方
     */
    private String nickName;
    /**
     * 客户端发送消息时间戳
     */
    private Long sendTime;
    /**
     * 服务端推送消息时间戳
     */
    private Long pushTime;

}

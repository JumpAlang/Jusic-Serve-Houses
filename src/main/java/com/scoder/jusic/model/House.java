package com.scoder.jusic.model;

import lombok.*;

import java.io.Serializable;

/**
 * @author alang
 * @create 2020-05-20 17:28
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class House implements Serializable {
    private static final long serialVersionUID = 1815307431423026256L;

    private String id;
    /**
     * WebSocketServerSockJsSession 中的 session id，创建者sessionId
     */
    private String sessionId;
    /**
     * 房间名称
     */
    private String name;
    /**
     * 房间描述
     */
    private String desc;
    /**
     * ip 地址
     */
    private String remoteAddress;

    /**
     * 创建时间
     */
    private Long createTime;


    /**
     * 房间密码
     */
    private String password;

    /**
     * 是否可用
     */
    private Boolean enableStatus;

    private Boolean needPwd;

    private Integer population;

    private Boolean canDestroy;

    private String retainKey;

    private Message announce;

    private Boolean forbiddenModiPwd;

    private String adminPwd;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        House house = (House) o;

        return id.equals(house.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

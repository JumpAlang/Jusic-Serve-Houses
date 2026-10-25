package com.scoder.jusic.model;

import com.scoder.jusic.util.StringUtils;
import lombok.*;

import java.io.Serializable;

/**
 * @author H
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -5508341219684417455L;

    private String houseId;
    /**
     * WebSocketServerSockJsSession 中的 session id
     */
    private String sessionId;
    /**
     * 用户名
     */
    private String name = "";
    /**
     * 昵称
     */
    private String nickName = "";
    /**
     * ip 地址
     */
    private String remoteAddress = "";
    /**
     * 角色
     */
    private String role = "default";
    /** Time when this connection entered the current room. */
    private Long joinedAt;
    /**
     * 最后时间..
     */
    private Long lastMessageTime;

    public String getNickName() {
        String maskedAddress = StringUtils.desensitizeIPV4(this.remoteAddress);
        StringBuilder nickName = new StringBuilder();
        nickName.append(this.name == null ? "" : this.name)
                .append("(")
                .append(maskedAddress == null ? "" : maskedAddress)
                .append(")");
        return nickName.toString();
    }
}

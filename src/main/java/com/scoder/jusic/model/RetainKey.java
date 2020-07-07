package com.scoder.jusic.model;

import lombok.*;

import java.io.Serializable;

/**
 * @author JumpAlang
 * @create 2020-07-06 16:15
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RetainKey implements Serializable {

    private static final long serialVersionUID = 6525829460045837065L;

    private Boolean isUsed;

    private String key;

    private Long createTime;

    private Long expireTime;

    private String houseId;
    /**
     * ip 地址
     */
    private String remoteAddress = "";

    private Long usedTime;

}

package com.scoder.jusic.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author H
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class Online extends Message {
    /**
     * 在线人数
     */
    private Integer count;
}

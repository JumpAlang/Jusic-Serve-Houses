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
public class Chat extends Message {
    private String type = "chat";
    private String sessionId;
}

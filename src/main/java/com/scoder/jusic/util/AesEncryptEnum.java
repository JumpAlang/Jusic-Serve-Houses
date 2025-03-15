package com.scoder.jusic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author JumpAlang
 * @create 2024-11-18 23:30
 */
@Getter
@AllArgsConstructor
public enum AesEncryptEnum {

    CBC("CBC"), ECB("ECB");

    private final String type;

}
package com.scoder.jusic.model;

import lombok.*;

/**
 * @author H
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Auth extends Message {
    private String password;
}

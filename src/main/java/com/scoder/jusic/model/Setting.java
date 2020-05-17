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
public class Setting extends Message {
    private String name;
}

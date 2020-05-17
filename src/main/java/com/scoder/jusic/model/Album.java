package com.scoder.jusic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 专辑
 *
 * @author H
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Album implements Serializable {

    private static final long serialVersionUID = -8508341219684417455L;
    /**
     * 专辑 id
     */
    private Integer id;
    /**
     * 专辑名
     */
    private String name;
    /**
     * 艺人
     */
    private String artist;
    /**
     * 专辑图片
     */
    private String pictureUrl;
}

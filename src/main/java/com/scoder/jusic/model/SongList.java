package com.scoder.jusic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author JumpAlang
 * @create 2020-06-08 14:42
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SongList implements Serializable {

    private static final long serialVersionUID= -4178319924735093771L;

    private String id;
    private String name;
    private String desc;
    private String creator;
    private String creatorUid;
    private Integer bookCount;
    private Integer playCount;
    private Integer songCount;
    private String source="wy";
    private String pictureUrl;
}

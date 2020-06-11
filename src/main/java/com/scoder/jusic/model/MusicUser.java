package com.scoder.jusic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author JumpAlang
 * @create 2020-06-08 14:42
 * 网易云用户
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MusicUser implements Serializable {

    private static final long serialVersionUID= -1563611325682129281L;

    private String avatarUrl;
    private String userId;
    private String nickname;
    private String sinature;
    private String description;
    private Integer gender;
    private String detailDescription;
    private String backgroundUrl;
    private String source="wy";
}

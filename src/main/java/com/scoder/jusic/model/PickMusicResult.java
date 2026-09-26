package com.scoder.jusic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickMusicResult {

    private boolean success;
    private String message;
    private Music music;
    private LinkedList<Music> pickList;

    public static PickMusicResult success(Music music, LinkedList<Music> pickList) {
        return new PickMusicResult(true, "点歌成功", music, pickList);
    }

    public static PickMusicResult failure(String message) {
        return new PickMusicResult(false, message, null, null);
    }
}

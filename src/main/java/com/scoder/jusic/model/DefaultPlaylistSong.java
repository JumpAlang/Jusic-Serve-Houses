package com.scoder.jusic.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Persistent display metadata, deliberately excluding audio URLs and cookies. */
@Data
public class DefaultPlaylistSong {
    private String id;
    private String source;
    private String name;
    private String artist;
    private String album;
    private Long duration;
    private String pictureUrl;

    public String member() { return id + ("qq".equals(source) ? "___qq" : ""); }

    public static DefaultPlaylistSong legacy(String member) {
        DefaultPlaylistSong song = new DefaultPlaylistSong();
        boolean qq = member.endsWith("___qq");
        song.setId(qq ? member.substring(0, member.length() - 5) : member);
        song.setSource(qq ? "qq" : "wy");
        return song;
    }

    public static DefaultPlaylistSong fromProvider(JSONObject raw, String source) {
        DefaultPlaylistSong song = new DefaultPlaylistSong();
        song.setSource(source);
        boolean qq = "qq".equals(source);
        song.setId(qq ? first(raw.getString("songmid"), raw.getString("mid")) : raw.getString("id"));
        song.setName(first(raw.getString("songname"), raw.getString("name")));
        JSONArray singers = qq ? raw.getJSONArray("singer") : raw.getJSONArray("ar");
        if (singers == null) singers = raw.getJSONArray("artists");
        List<String> names = new ArrayList<>();
        if (singers != null) for (int i = 0; i < singers.size(); i++) {
            String name = singers.getJSONObject(i).getString("name");
            if (name != null && !name.isEmpty()) names.add(name);
        }
        song.setArtist(String.join(" / ", names));
        JSONObject album = raw.getJSONObject(qq ? "album" : "al");
        if (album == null) album = raw.getJSONObject("album");
        song.setAlbum(first(raw.getString("albumname"), album == null ? null : album.getString("name")));
        Long duration = raw.getLong(qq ? "interval" : "dt");
        if (duration == null && !qq) duration = raw.getLong("duration");
        song.setDuration(duration == null ? null : qq ? duration * 1000L : duration);
        if (qq) {
            String mid = first(raw.getString("albummid"), album == null ? null : album.getString("mid"));
            mid = first(mid, raw.getString("albumMid"));
            if (mid != null) song.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + mid + ".jpg");
        } else if (album != null) song.setPictureUrl(album.getString("picUrl"));
        return song;
    }

    private static String first(String a, String b) { return a == null || a.isEmpty() ? b : a; }
}

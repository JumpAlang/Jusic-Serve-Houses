package com.scoder.jusic.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.DefaultPlaylistSong;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.service.imp.MusicServiceImpl;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
public class DefaultPlaylistImporter {
    private final MusicDefaultRepository repository;
    private final JusicProperties properties;

    public DefaultPlaylistImporter(MusicDefaultRepository repository, JusicProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public int append(String houseId, String[] playlistIds, String source) {
        if (!"wy".equals(source) && !"qq".equals(source))
            throw new IllegalArgumentException("默认列表仅支持网易和 QQ");
        int count = 0;
        for (String value : playlistIds) {
            checkInterrupted();
            String id = value.trim();
            boolean single = id.startsWith("*");
            if (single) id = id.substring(1);
            List<DefaultPlaylistSong> songs;
            if ("wy".equals(source)) songs = netease(id, single);
            else songs = qq(id, single);
            checkInterrupted();
            Long added = repository.addSongs(songs, houseId);
            count += added == null ? 0 : added.intValue();
        }
        return count;
    }

    private List<DefaultPlaylistSong> netease(String id, boolean single) {
        LinkedHashMap<String, DefaultPlaylistSong> songs = new LinkedHashMap<>();
        if (single) songs.put(id, DefaultPlaylistSong.legacy(id));
        else {
            JSONObject root = checked(Unirest.post(properties.getMusicServeDomain()+"/playlist/detail")
                    .queryString("id", id).queryString("cookie", MusicServiceImpl.NETEASE_COOKIE).asString(), "code", 200);
            JSONObject playlist = root.getJSONObject("playlist");
            if (playlist == null) throw new IllegalStateException("网易歌单数据为空");
            JSONArray ids = playlist.getJSONArray("trackIds");
            if (ids != null) for (int i = 0; i < ids.size(); i++) {
                String songId = ids.getJSONObject(i).getString("id");
                if (songId != null) songs.put(songId, DefaultPlaylistSong.legacy(songId));
            }
            mergeNetease(songs, playlist.getJSONArray("tracks"), ids == null);
        }
        List<String> missing = new ArrayList<>();
        for (DefaultPlaylistSong song : songs.values()) if (song.getName() == null) missing.add(song.getId());
        for (int start = 0; start < missing.size(); start += 100) {
            checkInterrupted();
            try {
                JSONObject detail = checked(Unirest.post(properties.getMusicServeDomain()+"/song/detail")
                        .queryString("ids", String.join(",", missing.subList(start, Math.min(start+100, missing.size()))))
                        .queryString("cookie", MusicServiceImpl.NETEASE_COOKIE).asString(), "code", 200);
                mergeNetease(songs, detail.getJSONArray("songs"), false);
            } catch (RuntimeException ex) {
                checkInterrupted();
                log.warn("Default playlist metadata unavailable; keeping song IDs for this batch");
            }
        }
        return new ArrayList<>(songs.values());
    }

    private void mergeNetease(Map<String, DefaultPlaylistSong> songs, JSONArray raw, boolean acceptNew) {
        if (raw == null) return;
        for (int i = 0; i < raw.size(); i++) {
            DefaultPlaylistSong song = DefaultPlaylistSong.fromProvider(raw.getJSONObject(i), "wy");
            if (song.getId() != null && (acceptNew || songs.containsKey(song.getId()))) songs.put(song.getId(), song);
        }
    }

    private List<DefaultPlaylistSong> qq(String id, boolean single) {
        if (single) {
            try {
                JSONObject root = checked(Unirest.get(properties.getMusicServeDomainQq()+"/song")
                        .queryString("songmid", id).asString(), "result", 100);
                DefaultPlaylistSong song = DefaultPlaylistSong.fromProvider(root.getJSONObject("data").getJSONObject("track_info"), "qq");
                song.setId(id);
                return Collections.singletonList(song);
            } catch (RuntimeException ex) {
                checkInterrupted();
                return Collections.singletonList(DefaultPlaylistSong.legacy(id+"___qq"));
            }
        }
        JSONObject root = checked(Unirest.get(properties.getMusicServeDomainQq()+"/songlist")
                .queryString("id", id).asString(), "result", 100);
        JSONArray raw = root.getJSONObject("data").getJSONArray("songlist");
        if (raw == null) throw new IllegalStateException("QQ 歌单数据为空");
        List<DefaultPlaylistSong> songs = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) songs.add(DefaultPlaylistSong.fromProvider(raw.getJSONObject(i), "qq"));
        return songs;
    }

    private JSONObject checked(HttpResponse<String> response, String field, int code) {
        if (response.getStatus() != 200) throw new IllegalStateException("歌单平台暂不可用");
        JSONObject root = JSONObject.parseObject(response.getBody());
        if (root == null || !Integer.valueOf(code).equals(root.getInteger(field)))
            throw new IllegalStateException("歌单读取失败");
        return root;
    }

    private void checkInterrupted() {
        if (ProviderCallExecutor.shouldAbortCurrentCall()) throw new IllegalStateException("歌单导入已超时，请重试");
    }
}

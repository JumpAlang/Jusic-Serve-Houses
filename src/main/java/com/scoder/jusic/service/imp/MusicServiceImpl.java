package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.*;
import com.scoder.jusic.repository.*;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.util.FileOperater;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author H
 */
@Service
@Slf4j
public class MusicServiceImpl implements MusicService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicPickRepository musicPickRepository;
    @Autowired
    private MusicDefaultRepository musicDefaultRepository;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private MusicBlackRepository musicBlackRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private ResourceLoader resourceLoader;
    /**
     * 把音乐放进点歌列表
     */
    @Override
    public Music toPick(String sessionId, Music music,String houseId) {
        music.setSessionId(sessionId);
        music.setPickTime(System.currentTimeMillis());
        User user = sessionRepository.getSession(sessionId,houseId);
        music.setNickName(user==null?"":user.getNickName());
        musicPickRepository.leftPush(music,houseId);
        log.info("点歌成功, 音乐: {}, 已放入点歌列表", music.getName());
        return music;
    }

    /**
     * 音乐切换
     *
     * @return -
     */
    @Override
    public Music musicSwitch(String houseId) {
        Music result = null;
        if (musicPickRepository.size(houseId) < 1) {

            String defaultPlayListHouse = houseId;
            if(musicDefaultRepository.size(houseId) == 0){
                defaultPlayListHouse = "";
            }
            String keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
            log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
            if(keyword.endsWith("___qq")){
                result = this.getQQMusicById(keyword.substring(0,keyword.length()-5));
            }else{
                result = this.getWYMusicById(keyword);
            }
            while(result == null || result.getUrl() == null){
                musicDefaultRepository.remove(keyword,defaultPlayListHouse);
                log.info("该歌曲url为空:{}", keyword);
                if(musicDefaultRepository.size(houseId) == 0){
                    defaultPlayListHouse = "";
                }
                keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
                log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
                if(keyword.endsWith("___qq")){
                    result = this.getQQMusicById(keyword.substring(0,keyword.length()-5));
                }else{
                    result = this.getWYMusicById(keyword);
                }
            }
            result.setPickTime(System.currentTimeMillis());
            result.setNickName("system");
//            musicPickRepository.leftPush(result,houseId);
            musicPlayingRepository.leftPush(result,houseId);
        }else{
            if(configRepository.getRandomModel(houseId) == null || !configRepository.getRandomModel(houseId)) {
                result = musicPlayingRepository.pickToPlaying(houseId);
            }else{
                result = musicPlayingRepository.randomToPlaying(houseId);
            }
            result.setIps(null);
        }
        updateMusicUrl(result);
        musicPlayingRepository.keepTheOne(houseId);

        return result;
    }

    @Override
    public void updateMusicUrl(Music result){
        // 防止选歌的时间超过音乐链接的有效时长
        if (!"lz".equals(result.getSource()) && result.getPickTime() + jusicProperties.getMusicExpireTime() <= System.currentTimeMillis()) {
            String musicUrl;
            if("qq".equals(result.getSource())){
                musicUrl = this.getQQMusicUrl(result.getId());
            }else if("mg".equals(result.getSource())){
                musicUrl = this.getMGMusicUrl(result.getId(),result.getName());
            }else{
                musicUrl = this.getMusicUrl(result.getId());
            }
            if(musicUrl == null){
                musicUrl = this.getKwXmUrlIterator(result.getArtist()+"+"+result.getName());
            }
            if (Objects.nonNull(musicUrl)) {
                result.setUrl(musicUrl);
                log.info("音乐链接已超时, 已更新链接");
            } else {
                log.info("音乐链接更新失败, 接下来客户端音乐链接可能会失效, 请检查音乐服务");
            }
        }
    }

    /**
     * 获取点歌列表
     *
     * @return linked list
     */
    @Override
    public LinkedList<Music> getPickList(String houseId) {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        Music playing = musicPlayingRepository.getPlaying(houseId);
        try{
            Collections.reverse(pickMusicList);
            result.add(playing);
            result.addAll(pickMusicList);


            result.forEach(m -> {
                // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
                m.setLyric("");
                m.setIps(null);
            });
        }catch(Exception e){
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    public Music getPlaying(String houseId) {
        Music playing = musicPlayingRepository.getPlaying(houseId);
        return playing;
    }

    @Override
    public LinkedList<Music> getSortedPickList(List<Music> musicList,String houseId) {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicList;//musicPickRepository.getPickMusicList();
        Collections.sort(pickMusicList,new MusicComparator());
        musicPickRepository.reset(houseId);
        musicPickRepository.rightPushAll(houseId,pickMusicList.toArray());
        Music playing = musicPlayingRepository.getPlaying(houseId);
        Collections.reverse(pickMusicList);
        result.add(playing);
        result.addAll(pickMusicList);

        result.forEach(m -> {
            // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
            m.setLyric("");
            m.setIps(null);
        });
        return result;
    }

    public List<Music> getPickListNoPlaying(String houseId) {
        return musicPickRepository.getPickMusicList(houseId);
    }

    @Override
    public Long modifyPickOrder(LinkedList<Music> musicList,String houseId) {
        musicPickRepository.reset(houseId);
        return musicPickRepository.leftPushAll(houseId,musicList);
    }

    /**
     * 投票
     *
     * @return 失败 = 0, 成功 >= 1
     */
    @Override
    public Long vote(String sessionId,String houseId) {
        return musicVoteRepository.add(houseId,sessionId);
    }

    /**
     * 从 redis set 中获取参与投票的人数
     *
     * @return 参与投票人数
     */
    @Override
    public Long getVoteCount(String houseId) {
        return musicVoteRepository.size(houseId);
    }

    /**
     * 获取音乐
     * </p>
     * 外链, 歌词, 艺人, 专辑, 专辑图片, 时长
     *
     * @param keyword 音乐关键字 | 网易云音乐 id
     * @return 音乐信息
     */
    @Override
    public Music getMusic(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/netease/song/" + keyword)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}, response: {}", jsonObject.get("message"), jsonObject);
                    if (jsonObject.get("code").equals(1)) {
                        music = JSONObject.parseObject(jsonObject.get("data").toString(), Music.class);
                        break;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    @Override
    public Music getQQMusic(String keyword){
        Music pick = null;
        if(keyword != null){
            if(StringUtils.isQQMusicId(keyword)){
                pick = this.getQQMusicById(keyword);
            }else{
                pick = this.getQQMusicByName(keyword);
            }
        }
        return pick;
    }
    @Override
    public Music getWYMusic(String keyword){
        Music pick = null;
        if(keyword != null){
            if(StringUtils.isWYMusicId(keyword)){
                pick = this.getWYMusicById(keyword);
            }else{
                pick = this.getWYMusicByName(keyword);
            }
        }
        return pick;
    }

    private Music getQQMusicByName(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song/find?key="+StringUtils.encodeString(keyword))
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("qq");
                        String id = data.getString("songmid");
                        music.setId(id);
                        String lyrics = getQQLyrics(id);
                        music.setLyric(lyrics);
                        String name = data.getString("songname");
                        music.setName(name);
                        JSONArray singerArray = data.getJSONArray("singer");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for(int j = 0; j < singerSize; j++){
                            singerNames += singerArray.getJSONObject(j).getString("name")+",";
                        }
                        if(singerNames.endsWith(",")){
                            singerNames = singerNames.substring(0,singerNames.length()-1);
                        }
                        music.setArtist(singerNames);
                        String url = data.getString("url");
                        if(url == null){
                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
                        }
                        music.setUrl(url);
                        long duration = data.getLong("interval")*1000;
                        music.setDuration(duration);
                        Album album = new Album();
                        Integer albumid = data.getInteger("albumid");
                        album.setId(albumid);
                        String albumname = data.getString("albumname");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String albummid = data.getString("albummid");
                        album.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                        music.setAlbum(album);
                        music.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                        return music;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    private String getKwXmUrlIterator(String keyword){
        String result = this.getKwXmUrl(keyword,"kuwo");
        if(result == null || result.indexOf("http") == -1){
            result = this.getKwXmUrl(keyword,"xiami");
        }
        return result;
    }

    private String getKwXmUrl(String keyword,String provider) {
        HttpResponse<String> response = null;
        try {
                response = Unirest.get(jusicProperties.getMusicServeDomainKwXm() + "/api/searchsingle").queryString("provider",provider).queryString("keyword",keyword).asString();

                if (response.getStatus() != 200) {
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取酷狗虾米音乐结果：{}", jsonObject);
                    if (jsonObject.getString("code").equals("20000")) {
                        String result = jsonObject.getString("data");
                        return result;
                    }
                }
            } catch (Exception e) {
                log.error("酷狗虾米音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }

        return null;
    }

    private Music getWYMusicByName(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.post(jusicProperties.getMusicServeDomain() + "/search").queryString("limit",1).queryString("offset",0).queryString("keywords",keyword).asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject result = jsonObject.getJSONObject("result");
                        if(result.getInteger("songCount") > 0){
                            JSONObject data = result.getJSONArray("songs").getJSONObject(0);
                            String id = data.getString("id");
                            music = getWYMusicById(id);
//                            music.setId(id);
//                            String lyrics = getWYLyrics(id);
//                            music.setLyric(lyrics);
//                            String name = data.getString("name");
//                            music.setName(name);
//                            JSONArray singerArray = data.getJSONArray("artists");
//                            int singerSize = singerArray.size();
//                            String singerNames = "";
//                            for(int j = 0; j < singerSize; j++){
//                                singerNames += singerArray.getJSONObject(j).getString("name")+",";
//                            }
//                            if(singerNames.endsWith(",")){
//                                singerNames = singerNames.substring(0,singerNames.length()-1);
//                            }
//                            music.setArtist(singerNames);
//                            String url = getMusicUrl(id);
//                            music.setUrl(url);
//                            long duration = data.getLong("duration");
//                            music.setDuration(duration);
//                            Album album = new Album();
//                            JSONObject albumJSON = data.getJSONObject("album");
//                            Integer albumid = albumJSON.getInteger("id");
//                            album.setId(albumid);
//                            String albumname = albumJSON.getString("name");
//                            album.setName(albumname);
//                            album.setArtist(singerNames);
//                            album.setPictureUrl(albumJSON.getString("img1v1Url"));
//                            music.setAlbum(album);
//                            music.setPictureUrl(album.getPictureUrl());
                            return music;
                        }else{
                            return null;
                        }

                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }


    @Override
    public Music getLZMusic(Integer index) {
        Music music = null;
        String listStr = null;
        try {
            listStr = FileOperater.commonReadFile(resourceLoader.getResource(jusicProperties.getMusicJson()));
        } catch (IOException e) {
            log.error("读取文件失败，message:[{}]",e.getMessage());
        }
        if(listStr == null || "".equals(listStr)) {
            return null;
        }
        JSONArray musicList = JSONArray.parseArray(listStr);
        JSONObject data = musicList.getJSONObject(index-1);
        music = new Music();
        music.setSource("lz");
        String id = data.getString("id");
        music.setId(id);
        String lyrics = "";
        music.setLyric(lyrics);
        String name = data.getString("name");
        music.setName(name);
        String singerNames = data.getString("artist");
        music.setArtist(singerNames);
        String url = data.getString("url");
        music.setUrl(url);
        long duration = data.getDouble("duration").longValue();
        music.setDuration(duration);
        Album album = new Album();
        JSONObject albumJSON = data.getJSONObject("album");
        Integer albumid = albumJSON.getInteger("id");
        album.setId(albumid);
        String albumname = albumJSON.getString("name");
        album.setName(albumname);
        album.setArtist(singerNames);
        album.setPictureUrl(data.getString("picture_url"));
        music.setAlbum(album);
        music.setPictureUrl(data.getString("picture_url"));
        return music;
    }

    private Music getMGMusicByName(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.post(jusicProperties.getMusicServeDomainMg() + "/song/find").queryString("keyword",keyword)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("mg");
                        String id = data.getString("cid");
                        music.setId(id);
                        String lyrics = getMGLyrics(id);
                        music.setLyric(lyrics);
                        String durationStr = data.getString("duration");
                        if(durationStr != null && !"".equals(durationStr)){
                            music.setDuration(StringUtils.strToMillisSecond(durationStr));
                        }else{
                            music.setDuration(StringUtils.getLyricsDuration(lyrics)+20000);
                        }
                        String name = data.getString("name");
                        music.setName(name);
                        JSONArray singerArray = data.getJSONArray("artists");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for(int j = 0; j < singerSize; j++){
                            singerNames += singerArray.getJSONObject(j).getString("name")+",";
                        }
                        if(singerNames.endsWith(",")){
                            singerNames = singerNames.substring(0,singerNames.length()-1);
                        }
                        music.setArtist(singerNames);
                        String url = data.getString("128k");
                        if(url == null){
                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
                        }
                        music.setUrl(url);

                        Album album = new Album();
                        JSONObject albumObject = data.getJSONObject("album");
                        Integer albumid = albumObject.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumObject.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String picUrl = albumObject.getString("picUrl");
                        album.setPictureUrl(picUrl);
                        music.setAlbum(album);
                        music.setPictureUrl(picUrl);
                        return music;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("mg音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    private String getQQLyrics(String id){
        HttpResponse<String> response = null;
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                Unirest.setTimeouts(10000,15000);

                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/lyric?songmid=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        return data.getString("lyric");
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return "";
    }
    private String getWYLyrics(String id){
        HttpResponse<String> response = null;
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                Unirest.setTimeouts(10000,15000);
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/lyric?id=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject data = jsonObject.getJSONObject("lrc");
                        return data.getString("lyric");
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("网易音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return "";
    }

    private String getMGLyrics(String id){
        HttpResponse<String> response = null;
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                Unirest.setTimeouts(10000,15000);
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/lyric?cid=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        return jsonObject.getString("data");
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return "";
    }

    @Override
    public Music getQQMusicById(String id) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song?songmid=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("qq");
                        music.setId(id);
                        String lyrics = getQQLyrics(id);
                        music.setLyric(lyrics);
                        JSONObject trackInfoJSON = data.getJSONObject("track_info");
                        String name = trackInfoJSON.getString("name");
                        music.setName(name);
                        JSONArray singerArray = trackInfoJSON.getJSONArray("singer");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for(int j = 0; j < singerSize; j++){
                            singerNames += singerArray.getJSONObject(j).getString("name")+",";
                        }
                        if(singerNames.endsWith(",")){
                            singerNames = singerNames.substring(0,singerNames.length()-1);
                        }
                        music.setArtist(singerNames);
                        String url = getQQMusicUrl(id);
                        if(url == null){
                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
                        }
                        music.setUrl(url);
                        long duration = trackInfoJSON.getLong("interval")*1000;
                        music.setDuration(duration);
                        Album album = new Album();
                        JSONObject albumJSON = trackInfoJSON.getJSONObject("album");
                        Integer albumid = albumJSON.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumJSON.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String albummid = albumJSON.getString("mid");
                        album.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                        music.setAlbum(album);
                        music.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                        return music;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    @Override
    public Music getWYMusicById(String id) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/song/detail?ids=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("code").equals(200)) {
                        JSONArray songs = jsonObject.getJSONArray("songs");
                        JSONObject song = songs.getJSONObject(0);
                        music = new Music();
                        music.setSource("wy");
                        music.setId(id);
                        String lyrics = getWYLyrics(id);
                        music.setLyric(lyrics);
                        String name = song.getString("name");
                        music.setName(name);
                        JSONArray singerArray = song.getJSONArray("ar");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for(int j = 0; j < singerSize; j++){
                            singerNames += singerArray.getJSONObject(j).getString("name")+",";
                        }
                        if(singerNames.endsWith(",")){
                            singerNames = singerNames.substring(0,singerNames.length()-1);
                        }
                        music.setArtist(singerNames);
                        String url = getMusicUrl(id);
                        if(url == null){
                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
                        }
                        music.setUrl(url);
                        long duration = song.getLong("dt");
                        music.setDuration(duration);
                        Album album = new Album();
                        JSONObject albumJSON = song.getJSONObject("al");
                        Integer albumid = albumJSON.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumJSON.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        album.setPictureUrl(albumJSON.getString("picUrl"));
                        music.setAlbum(album);
                        music.setPictureUrl(album.getPictureUrl());
                        return music;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    private JSONArray  getWYMusicsById(String ids) {
        HttpResponse<String> response = null;
        JSONArray buildJSONArray = new JSONArray();

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/song/detail?ids=" + ids)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject json = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (json.get("code").equals(200)) {
                        buildJSONArray = new JSONArray();
                        JSONArray songs = json.getJSONArray("songs");
                        int size = songs.size();
                        for(int i = 0; i < size; i++){
                            JSONObject jsonObject = songs.getJSONObject(i);
                            JSONObject buildJSONObject = new JSONObject();
                            JSONObject albumJSON = jsonObject.getJSONObject("al");
                            JSONObject album = new JSONObject();
                            String albumid = albumJSON.getString("id");
                            String albumname = albumJSON.getString("name");
                            album.put("id",albumid);
                            album.put("name",albumname);
                            album.put("picture_url",albumJSON.getString("picUrl"));
                            JSONArray singerArray = jsonObject.getJSONArray("ar");
                            int singerSize = singerArray.size();
                            String singerNames = "";
                            for(int j = 0; j < singerSize; j++){
                                singerNames += singerArray.getJSONObject(j).getString("name")+",";
                            }
                            if(singerNames.endsWith(",")){
                                singerNames = singerNames.substring(0,singerNames.length()-1);
                            }
                            buildJSONObject.put("picture_url","");
                            buildJSONObject.put("artist",singerNames);
                            String songname = jsonObject.getString("name");
                            buildJSONObject.put("name",songname);
                            String songmid = jsonObject.getString("id");
                            buildJSONObject.put("id",songmid);
                            int interval = jsonObject.getInteger("dt");
                            buildJSONObject.put("duration",interval);
                            JSONObject privilege = new JSONObject();
                            int fee = jsonObject.getInteger("fee");
//                    if(fee == 0){
//                        privilege.put("st",0);
//                        privilege.put("fl",0);
//                    }else{
                            privilege.put("st",1);
                            privilege.put("fl",1);
//                    }
                            buildJSONObject.put("privilege",privilege);

                            buildJSONObject.put("album",album);
                            buildJSONArray.add(buildJSONObject);
                        }
                        return buildJSONArray;
                    }else{
                        return buildJSONArray;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return buildJSONArray;
    }

    @Override
    public Music getMGMusic(String keyword) {
        Music pick = null;
        if(keyword != null){
            if(StringUtils.isMGMusicId(keyword)){
                pick = this.getMGMusicById(keyword);
            }else{
                pick = this.getMGMusicByName(keyword);
            }
        }
        return pick;    }

    @Override
    public Music getMGMusicById(String id) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/song?id=" + id+"&cid="+id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("mg");
                        music.setId(id);
                        String lyrics = getMGLyrics(id);
                        music.setLyric(lyrics);
                        String durationStr = data.getString("duration");
                        if(durationStr != null && !"".equals(durationStr)){
                            music.setDuration(StringUtils.strToMillisSecond(durationStr));
                        }else{
                            music.setDuration(StringUtils.getLyricsDuration(lyrics)+20000);
                        }
                        String name = data.getString("name");
                        music.setName(name);
                        JSONArray singerArray = data.getJSONArray("artists");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for(int j = 0; j < singerSize; j++){
                            singerNames += singerArray.getJSONObject(j).getString("name")+",";
                        }
                        if(singerNames.endsWith(",")){
                            singerNames = singerNames.substring(0,singerNames.length()-1);
                        }
                        music.setArtist(singerNames);
                        String url = data.getString("128k");
                        if(url == null){
                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
                        }
                        music.setUrl(url);

                        Album album = new Album();
                        JSONObject albumObject = data.getJSONObject("album");
                        Integer albumid = albumObject.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumObject.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String picUrl = data.getString("picUrl");
                        album.setPictureUrl(picUrl);
                        music.setAlbum(album);
                        music.setPictureUrl(picUrl);
                        return music;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    @Override
    public String getMusicUrl(String musicId) {
        HttpResponse<String> response = null;
        String result = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/song/url?br=128000&id=" + musicId + "")
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐链接结果：{}, response: {}", jsonObject.get("message"), jsonObject);
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
                        result = data.getString("url");
                        break;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public String getQQMusicUrl(String musicId) {
        HttpResponse<String> response = null;
        String result = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song/urls?id="+musicId)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐链接结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString(musicId);
                        break;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public String getMGMusicUrl(String musicId, String musicName) {
        HttpResponse<String> response = null;
        String result = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/song/url?id="+musicId+"&cid="+musicId+"&songName="+ URLEncoder.encode(musicName))
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐链接结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString("128k");
                        break;
                    }else{
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public boolean deletePickMusic(Music music,String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        boolean isDeleted = false;
        for (int i = 0; i < pickMusicList.size(); i++) {
            if(music.getSessionId() != null){
                if (pickMusicList.get(i).getName().equals(music.getName()) && music.getSessionId().equals(pickMusicList.get(i).getSessionId())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            }else{
                if (music.getId().equals(pickMusicList.get(i).getId()) || pickMusicList.get(i).getName().equals(music.getName())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            }
        }
        if(isDeleted){
            musicPickRepository.reset(houseId);
            if(pickMusicList != null && pickMusicList.size() != 0){
                musicPickRepository.rightPushAll(houseId,pickMusicList.toArray());
            }
        }
       return  isDeleted;
    }

    @Override
    public void topPickMusic(Music music,String houseId) {
        List<Music> newPickMusicList = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (int i = 0; i < pickMusicList.size(); i++) {
            if (music.getId().equals(pickMusicList.get(i).getId())) {
                Music music2 = pickMusicList.get(i);
                music2.setTopTime(System.currentTimeMillis());
                newPickMusicList.add(music2);
                pickMusicList.remove(pickMusicList.get(i));
                break;
            }
        }
        pickMusicList.addAll(newPickMusicList);
        musicPickRepository.reset(houseId);
        musicPickRepository.rightPushAll(houseId,pickMusicList.toArray());
    }

    @Override
    public Long black(String id,String houseId) {
        return musicBlackRepository.add(id,houseId);
    }

    @Override
    public Long unblack(String id,String houseId) {
        return musicBlackRepository.remove(id,houseId);
    }

    @Override
    public boolean isBlack(String id,String houseId) {
        return musicBlackRepository.isMember(id,houseId);
    }

    @Override
    public boolean isPicked(String id,String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return true;
            }
        }
        Music playing = musicPlayingRepository.getPlaying(houseId);
        return playing.getId().equals(id);
    }

    public Object[] getMusicById(String id,String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return new Object[]{music,pickMusicList};
            }
        }
        return null;
    }

    @Override
    public HulkPage search(Music music, HulkPage hulkPage) {
        if(music.getSource().equals("qq")){
            if("*热歌榜".equals(music.getName())){
                return searchQQTop(hulkPage);
            }
            else if(StringUtils.isGDMusicId(music.getName())){
                return searchQQGD(music.getName().substring(1),hulkPage);
            }else{
                return searchQQ(music,hulkPage);
            }
        }else if(music.getSource().equals("mg")){
            return searchMG(music,hulkPage);
        }else if(music.getSource().equals("lz")){
            return searchLZ(music,hulkPage);
        }else{
            if("*热歌榜".equals(music.getName())){
                return searchWYGD(jusicProperties.getWyTopUrl(),hulkPage);
            }else if(StringUtils.isGDMusicId(music.getName())){
                return searchWYGD(music.getName().substring(1),hulkPage);
            }else{
                return searchWY(music,hulkPage);
            }
        }
//        StringBuilder url = new StringBuilder()
//                .append(jusicProperties.getMusicServeDomain())
//                .append("/netease/songs/")
//                .append(music.getName())
//                .append("/search")
//                .append("/").append(hulkPage.getPageIndex() - 1)
//                .append("/").append(hulkPage.getPageSize());
//        HttpResponse<String> response = null;
//        try {
//            response = Unirest.get(url.toString())
//                    .asString();
//            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
//            if (responseJsonObject.getInteger("code") == 1) {
//                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("data");
//                Integer count = responseJsonObject.getJSONObject("data").getInteger("count");
//                List list = JSONObject.parseObject(JSONObject.toJSONString(data), List.class);
//                hulkPage.setData(list);
//                hulkPage.setTotalSize(count);
//            } else {
//                log.info("音乐搜索接口异常, 请检查音乐服务");
//            }
//        } catch (Exception e) {
//            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
//        }
//        return hulkPage;
    }

    private HulkPage searchQQ(Music music,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/search?key=")
                .append(StringUtils.encodeString(music.getName()))
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albummid");
                    buildJSONObject.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for(int j = 0; j < singerSize; j++){
                        singerNames += singerArray.getJSONObject(j).getString("name")+",";
                    }
                    if(singerNames.endsWith(",")){
                        singerNames = singerNames.substring(0,singerNames.length()-1);
                    }
                    buildJSONObject.put("artist",singerNames);
                    String songname = jsonObject.getString("songname");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("songmid");
                    buildJSONObject.put("id",songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration",interval*1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st",1);
                    privilege.put("fl",1);
                    buildJSONObject.put("privilege",privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    String albumid = jsonObject.getString("albumid");
                    String albumname = jsonObject.getString("albumname");
                    album.put("id",albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchQQGD(String id,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/songlist?id=")
                .append(id);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("songlist");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                int offset = (hulkPage.getPageIndex()-1)*hulkPage.getPageSize();
                int pages = (size+hulkPage.getPageSize()-1)/hulkPage.getPageSize();
                if(hulkPage.getPageIndex() > pages){
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return  hulkPage;
                }
                for(int i = offset; i < (hulkPage.getPageIndex()==pages?size:hulkPage.getPageIndex()*hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albummid");
                    buildJSONObject.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for(int j = 0; j < singerSize; j++){
                        singerNames += singerArray.getJSONObject(j).getString("name")+",";
                    }
                    if(singerNames.endsWith(",")){
                        singerNames = singerNames.substring(0,singerNames.length()-1);
                    }
                    buildJSONObject.put("artist",singerNames);
                    String songname = jsonObject.getString("songname");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("songmid");
                    buildJSONObject.put("id",songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration",interval*1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st",1);
                    privilege.put("fl",1);
                    buildJSONObject.put("privilege",privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    String albumid = jsonObject.getString("albumid");
                    String albumname = jsonObject.getString("albumname");
                    album.put("id",albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchQQTop(HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/top?id=26")
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                responseJsonObject = responseJsonObject.getJSONObject("data");
                JSONArray data = responseJsonObject.getJSONArray("list");
                Integer count = responseJsonObject.getInteger("total");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albumMid");
                    buildJSONObject.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for(int j = 0; j < singerSize; j++){
                        singerNames += singerArray.getJSONObject(j).getString("name")+",";
                    }
                    if(singerNames.endsWith(",")){
                        singerNames = singerNames.substring(0,singerNames.length()-1);
                    }
                    buildJSONObject.put("artist",singerNames);
                    String songname = jsonObject.getString("name");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("mid");
                    buildJSONObject.put("id",songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration",interval*1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st",1);
                    privilege.put("fl",1);
                    buildJSONObject.put("privilege",privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url","https://y.gtimg.cn/music/photo_new/T002R300x300M000"+albummid+".jpg");
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    String albumid = albumObject.getString("mid");
                    String albumname = albumObject.getString("name");
                    album.put("id",albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private String[] searchQQGD(String id) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/songlist?id=")
                .append(id);
        HttpResponse<String> response = null;
        ArrayList<String> ids = new ArrayList<>();
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("songlist");
                int size = data.size();
                if(size != 0) {
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = data.getJSONObject(i);
                        String songmid = jsonObject.getString("songmid")+"___qq";
                        ids.add(songmid);
                    }
                }else{
                    return new String[]{};
                }
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            return null;
        }
        String[] idsStr = new String[ids.size()];
        ids.toArray(idsStr);
        return idsStr;
    }

    private HulkPage searchWY(Music music,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("keywords",music.getName()).queryString("offset",(hulkPage.getPageIndex()-1)*hulkPage.getPageSize()).queryString("limit",hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("songs");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    JSONArray singerArray = jsonObject.getJSONArray("artists");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for(int j = 0; j < singerSize; j++){
                        singerNames += singerArray.getJSONObject(j).getString("name")+",";
                    }
                    if(singerNames.endsWith(",")){
                        singerNames = singerNames.substring(0,singerNames.length()-1);
                    }
                    buildJSONObject.put("picture_url","");
                    buildJSONObject.put("artist",singerNames);
                    String songname = jsonObject.getString("name");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("id");
                    buildJSONObject.put("id",songmid);
                    int interval = jsonObject.getInteger("duration");
                    buildJSONObject.put("duration",interval);
                    JSONObject privilege = new JSONObject();
                    int fee = jsonObject.getInteger("fee");
//                    if(fee == 0){
//                        privilege.put("st",0);
//                        privilege.put("fl",0);
//                    }else{
                        privilege.put("st",1);
                        privilege.put("fl",1);
//                    }
                    buildJSONObject.put("privilege",privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url","");
                    String albumid = albumObject.getString("id");
                    String albumname = jsonObject.getString("name");
                    album.put("id",albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("result").getInteger("songCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 网易歌单
     * @param id
     * @param hulkPage
     * @return
     */
    private HulkPage searchWYGD(String id,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/playlist/detail?id=")
                .append(id);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                int size = data.size();
                int offset = (hulkPage.getPageIndex()-1)*hulkPage.getPageSize();
                int pages = (size+hulkPage.getPageSize()-1)/hulkPage.getPageSize();
                if(hulkPage.getPageIndex() > pages){
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return  hulkPage;
                }
                Set<String> ids = new LinkedHashSet<>();
                for(int i = offset; i < (hulkPage.getPageIndex()==pages?size:hulkPage.getPageIndex()*hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    ids.add(jsonObject.getString("id"));
                }
                if(ids.size() > 0){
                    String idsStr = String.join(",",ids);
                    List list = JSONObject.parseObject(JSONObject.toJSONString( getWYMusicsById(idsStr)), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                }
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }
    private String[] searchWYGD(String id) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/playlist/detail?id=")
                .append(id);
        HttpResponse<String> response = null;
        ArrayList<String> ids = new ArrayList();
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                if(data == null){
                    return new String[]{};
                }
                int size = data.size();

                for(int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    ids.add(jsonObject.getString("id"));
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            return null;
        }
        String[] idsStr = new String[ids.size()];
        ids.toArray(idsStr);
        return idsStr;
    }

    /**

     *     * 网易歌单搜索 @param songList
     * @param hulkPage
     * @return
     */
    private HulkPage searchWYGD(SongList songList, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("type",1000).queryString("keywords",songList.getName()).queryString("offset",(hulkPage.getPageIndex()-1)*hulkPage.getPageSize()).queryString("limit",hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("playlists");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("name"));
                    buildJSONObject.put("desc",jsonObject.getString("description"));
                    buildJSONObject.put("id",jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount",jsonObject.getInteger("bookCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator",creator.getString("nickname"));
                    buildJSONObject.put("creatorUid",creator.getString("userId"));
                    buildJSONObject.put("songCount",jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("result").getInteger("playlistCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchWYGDAll(HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/top/playlist?order=hot")
                .append("&offset=").append((hulkPage.getPageIndex()-1)*hulkPage.getPageSize())
                .append("&limit=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONArray("playlists");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("name"));
                    buildJSONObject.put("desc",jsonObject.getString("description"));
                    buildJSONObject.put("id",jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount",jsonObject.getInteger("subscribedCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator",creator.getString("nickname"));
                    buildJSONObject.put("creatorUid",creator.getString("userId"));
                    buildJSONObject.put("songCount",jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchWYGDByUid(SongList songList,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/user/playlist?uid=")
                .append(songList.getName());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONArray("playlist");
                int size = data.size();
                int offset = (hulkPage.getPageIndex()-1)*hulkPage.getPageSize();
                int pages = (size+hulkPage.getPageSize()-1)/hulkPage.getPageSize();
                if(hulkPage.getPageIndex() > pages){
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return  hulkPage;
                }
                JSONArray buildJSONArray = new JSONArray();
                for(int i = offset; i < (hulkPage.getPageIndex()==pages?size:hulkPage.getPageIndex()*hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("name"));
                    buildJSONObject.put("desc",jsonObject.getString("description"));
                    buildJSONObject.put("id",jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount",jsonObject.getInteger("subscribedCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator",creator.getString("nickname"));
                    buildJSONObject.put("creatorUid",creator.getString("userId"));
                    buildJSONObject.put("songCount",jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);

                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * QQ歌单搜索
     * @param songList
     * @param hulkPage
     * @return
     */
    private HulkPage searchQQGD(SongList songList, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/search?t=2&key=")
                .append(StringUtils.encodeString(songList.getName()))
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("dissname"));
                    buildJSONObject.put("desc",jsonObject.getString("introduction"));
                    buildJSONObject.put("id",jsonObject.getString("dissid"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("imgurl"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("listennum"));
                    buildJSONObject.put("bookCount",null);
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator",creator.getString("name"));
                    buildJSONObject.put("creatorUid",creator.getString("qq"));
                    buildJSONObject.put("songCount",jsonObject.getInteger("song_count"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchQQGDAll(HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/songlist/list?category=10000000")
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("dissname"));
                    buildJSONObject.put("desc",jsonObject.getString("introduction"));
                    buildJSONObject.put("id",jsonObject.getString("dissid"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("imgurl"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("listennum"));
                    buildJSONObject.put("bookCount",null);
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator",creator.getString("name"));
                    buildJSONObject.put("creatorUid",creator.getString("qq"));
                    buildJSONObject.put("songCount",null);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchQQGDByUid(SongList songList,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/user/songlist?id=")
                .append(songList.getName());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                int offset = (hulkPage.getPageIndex()-1)*hulkPage.getPageSize();
                int pages = (size+hulkPage.getPageSize()-1)/hulkPage.getPageSize();
                if(hulkPage.getPageIndex() > pages){
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return  hulkPage;
                }
                JSONArray buildJSONArray = new JSONArray();
                JSONObject creatorJson = responseJsonObject.getJSONObject("data").getJSONObject("creator");
                String creator = creatorJson.getString("hostname");
                String creatorUid = creatorJson.getString("hostuin");

                for(int i = offset; i < (hulkPage.getPageIndex()==pages?size:hulkPage.getPageIndex()*hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name",jsonObject.getString("diss_name"));
                    buildJSONObject.put("desc","");
                    buildJSONObject.put("id",jsonObject.getString("tid"));
                    buildJSONObject.put("pictureUrl",jsonObject.getString("diss_cover"));
                    buildJSONObject.put("playCount",jsonObject.getInteger("listen_num"));
                    buildJSONObject.put("bookCount",null);
                    buildJSONObject.put("creator",creator);
                    buildJSONObject.put("creatorUid",creatorUid);
                    buildJSONObject.put("songCount",jsonObject.getInteger("song_cnt"));
                    buildJSONArray.add(buildJSONObject);

                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private JSONArray getCurrentPageList(int pageNo,int pageSize,JSONArray data){
        int size = data.size();
        int pages = (size+pageSize-1)/pageSize;
        if(pageNo > pages){
            return new JSONArray();
        }else{
            JSONArray pagedArray = new JSONArray();
            for(int i = (pageNo-1)*pageSize; i < (pageNo==pages?size:pageNo*pageSize); i++) {
                pagedArray.add(data.getJSONObject(i));
            }
            return pagedArray;
        }
    }


    private HulkPage searchLZ(Music music,HulkPage hulkPage) {
        String listStr = null;
        try {
            listStr = FileOperater.commonReadFile(resourceLoader.getResource(jusicProperties.getMusicJson()));
        } catch (IOException e) {
            log.error("读取文件失败，message:[{}]",e.getMessage());
        }
        if(listStr == null || "".equals(listStr)) {
            hulkPage.setTotalSize(0);
            hulkPage.setData(new Object[]{});
            return hulkPage;
        }
        JSONArray data = JSONArray.parseArray(listStr);
        int size = data.size();
        if(music.getName() == null || "".equals(music.getName())) {
            List list = JSONObject.parseObject(JSONObject.toJSONString(getCurrentPageList(hulkPage.getPageIndex(),hulkPage.getPageSize(),data)), List.class);
            hulkPage.setData(list);
            hulkPage.setTotalSize(size);
            return hulkPage;
        }
        JSONArray buildJSONArray = new JSONArray();
        for(int i = 0; i < size; i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            if (jsonObject.getString("artist").indexOf(music.getName()) != -1 || jsonObject.getString("name").indexOf(music.getName()) != -1 || jsonObject.getJSONObject("album").getString("name").indexOf(music.getName()) != -1) {
                buildJSONArray.add(jsonObject);
            }
        }
        if(buildJSONArray.size() > 0){
            List list = JSONObject.parseObject(JSONObject.toJSONString(getCurrentPageList(hulkPage.getPageIndex(),hulkPage.getPageSize(),buildJSONArray)), List.class);
            hulkPage.setTotalSize(buildJSONArray.size());
            hulkPage.setData(list);
        }else{
            hulkPage.setTotalSize(0);
            hulkPage.setData(new Object[]{});
            return hulkPage;
        }
        return hulkPage;
    }

    private HulkPage searchMG(Music music,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainMg())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("keyword",music.getName()).queryString("pageNo",hulkPage.getPageIndex()).queryString("pageSize",hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for(int i = 0; i < size; i++){
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    JSONArray singerArray = jsonObject.getJSONArray("artists");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for(int j = 0; j < singerSize; j++){
                        singerNames += singerArray.getJSONObject(j).getString("name")+",";
                    }
                    if(singerNames.endsWith(",")){
                        singerNames = singerNames.substring(0,singerNames.length()-1);
                    }
                    buildJSONObject.put("artist",singerNames);
                    String songname = jsonObject.getString("name");
                    buildJSONObject.put("name",songname);
                    String songmid = jsonObject.getString("cid");
                    buildJSONObject.put("id",songmid);
                    String interval = jsonObject.getString("duration");
                    if(interval != null){
                        buildJSONObject.put("duration",StringUtils.strToMillisSecond(interval));
                    }else{
                        buildJSONObject.put("duration",null);
                    }
                    JSONObject privilege = new JSONObject();
                    privilege.put("st",1);
                    privilege.put("fl",1);
                    buildJSONObject.put("privilege",privilege);

                    JSONObject album = new JSONObject();
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    String albumid = albumObject.getString("id");
                    String picUrl = albumObject.getString("picUrl");
                    String albumname = albumObject.getString("name");
                    buildJSONObject.put("picture_url",picUrl);
                    album.put("picture_url",picUrl);
                    album.put("id",albumid);
                    album.put("name",albumname);
                    buildJSONObject.put("album",album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("mg音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

     /*     * 网易用户搜索 @param musicUser
     * @param hulkPage
     * @return
             */
    private HulkPage searchWYGD(MusicUser musicUser, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("type",1002).queryString("keywords",musicUser.getNickname()).queryString("offset",(hulkPage.getPageIndex()-1)*hulkPage.getPageSize()).queryString("limit",hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("userprofiles");
                Integer count = responseJsonObject.getJSONObject("result").getInteger("userprofileCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(data), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("网易用户搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("网易用户搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }


    @Override
    public boolean clearPlayList(String houseId) {
        musicPickRepository.reset(houseId);
        return true;
    }

    @Override
    public String showBlackMusic(String houseId) {
        Set blackList = musicBlackRepository.showBlackList(houseId);
        if(blackList != null && blackList.size() > 0){
           return String.join(",",blackList);
        }
        return  null;
    }

    @Override
    public Page<List<SongList>> search(SongList songList, HulkPage hulkPage) {
        if("qq".equals(songList.getSource())){
            if(songList.getName() == null || songList.getName() == ""){
                return this.searchQQGDAll(hulkPage);
            }else{
                return this.searchQQGD(songList,hulkPage);
            }
        }else if("qq_user".equals(songList.getSource())){
            if(songList.getName() != null && StringUtils.isUserId(songList.getName()) ){
                return this.searchQQGDByUid(songList,hulkPage);
            }else{
                return hulkPage;
            }
        }else if("wy_user".equals(songList.getSource())){
            if(songList.getName() != null && StringUtils.isUserId(songList.getName()) ){
                return this.searchWYGDByUid(songList,hulkPage);
            }else{
                return hulkPage;
            }
        }else{
            if(songList.getName() == null || songList.getName() == ""){
                return this.searchWYGDAll(hulkPage);
            }else{
                return this.searchWYGD(songList,hulkPage);
            }
        }
    }

    @Override
    public Page<List<MusicUser>> search(MusicUser musicUser, HulkPage hulkPage) {
        if("qq".equals(musicUser.getSource())){
            return this.searchWYGD(musicUser,hulkPage);
        }else{
            return this.searchWYGD(musicUser,hulkPage);
        }
    }

    @Override
    public boolean clearDefaultPlayList(String houseId) {
        musicDefaultRepository.destroy(houseId);
        return true;
    }

    @Override
    public Integer addDefaultPlayList(String houseId, String[] playlistIds, String source) {
        if("wy".equals(source)){
            Integer count = 0;
            for(String id : playlistIds){
                String[] list = this.searchWYGD(id);
                if(list != null && list.length > 0){
                    musicDefaultRepository.add(list,houseId);
                    count += list.length;
                }
            }
            return count;
        }else if("qq".equals(source)){
            Integer count = 0;
            for(String id : playlistIds){
                String[] list = this.searchQQGD(id);
                if(list != null && list.length > 0){
                    musicDefaultRepository.add(list,houseId);
                    count += list.length;
                }
            }
            return count;
        }else{
            return null;
        }
    }

    @Override
    public Long playlistSize(String houseId) {
        return musicDefaultRepository.size(houseId);
    }

}

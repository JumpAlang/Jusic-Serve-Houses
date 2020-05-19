package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicComparator;
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
    private ResourceLoader resourceLoader;
    /**
     * 把音乐放进点歌列表
     */
    @Override
    public Music toPick(String sessionId, Music music) {
        music.setSessionId(sessionId);
        music.setPickTime(System.currentTimeMillis());
        music.setNickName(sessionRepository.getSession(sessionId).getNickName());
        musicPickRepository.leftPush(music);
        log.info("点歌成功, 音乐: {}, 已放入点歌列表", music.getName());
        return music;
    }

    /**
     * 音乐切换
     *
     * @return -
     */
    @Override
    public Music musicSwitch() {
        Music result = null;
        if (musicPickRepository.size() < 1) {
            String keyword = musicDefaultRepository.randomMember();
            log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
            if(keyword.endsWith("___qq")){
                result = this.getQQMusicById(keyword.substring(0,keyword.length()-5));
            }else{
                result = this.getMusic(keyword);
            }
            while(result.getUrl() == null){
                log.info("该歌曲url为空:{}", keyword);
                keyword = musicDefaultRepository.randomMember();
                log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
                result = this.getMusic(keyword);
            }
            result.setPickTime(System.currentTimeMillis());
            result.setNickName("system");
            musicPickRepository.leftPush(result);
        }

        result = musicPlayingRepository.pickToPlaying();
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
            if (Objects.nonNull(musicUrl)) {
                result.setUrl(musicUrl);
                log.info("音乐链接已超时, 已更新链接");
            } else {
                log.info("音乐链接更新失败, 接下来客户端音乐链接可能会失效, 请检查音乐服务");
            }
        }

        musicPlayingRepository.keepTheOne();

        return result;
    }

    /**
     * 获取点歌列表
     *
     * @return linked list
     */
    @Override
    public LinkedList<Music> getPickList() {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList();
        Music playing = musicPlayingRepository.getPlaying();
        Collections.reverse(pickMusicList);
        result.add(playing);
        result.addAll(pickMusicList);

        result.forEach(m -> {
            // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
            m.setLyric("");
        });
        return result;
    }

    @Override
    public Music getPlaying() {
        Music playing = musicPlayingRepository.getPlaying();
        return playing;
    }

    @Override
    public LinkedList<Music> getSortedPickList(List<Music> musicList) {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicList;//musicPickRepository.getPickMusicList();
        Collections.sort(pickMusicList,new MusicComparator());
        musicPickRepository.reset();
        musicPickRepository.rightPushAll(pickMusicList.toArray());
        Music playing = musicPlayingRepository.getPlaying();
        Collections.reverse(pickMusicList);
        result.add(playing);
        result.addAll(pickMusicList);

        result.forEach(m -> {
            // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
            m.setLyric("");
        });
        return result;
    }

    public List<Music> getPickListNoPlaying() {
        return musicPickRepository.getPickMusicList();
    }

    @Override
    public Long modifyPickOrder(LinkedList<Music> musicList) {
        musicPickRepository.reset();
        return musicPickRepository.leftPushAll(musicList);
    }

    /**
     * 投票
     *
     * @return 失败 = 0, 成功 >= 1
     */
    @Override
    public Long vote(String sessionId) {
        return musicVoteRepository.add(sessionId);
    }

    /**
     * 从 redis set 中获取参与投票的人数
     *
     * @return 参与投票人数
     */
    @Override
    public Long getVoteCount() {
        return musicVoteRepository.size();
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

    private Music getQQMusicByName(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song/find?key=" + keyword)
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
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/song/find?keyword=" + keyword)
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
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return "";
    }

    private String getMGLyrics(String id){
        HttpResponse<String> response = null;
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/lyric?cid=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        return jsonObject.getString("data");
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
                    log.info("获取音乐结果：{}", jsonObject);
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
                    log.info("获取音乐结果：{}", jsonObject);
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
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/netease/song/" + musicId + "/url")
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐链接结果：{}, response: {}", jsonObject.get("message"), jsonObject);
                    if (jsonObject.get("code").equals(1)) {
                        result = JSONObject.parseObject(jsonObject.get("data").toString()).get("url").toString();
                        break;
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
                    log.info("获取音乐链接结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString(musicId);
                        break;
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
                    log.info("获取音乐链接结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString("128k");
                        break;
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
    public boolean deletePickMusic(Music music) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList();
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
            musicPickRepository.reset();
            if(pickMusicList != null && pickMusicList.size() != 0){
                musicPickRepository.rightPushAll(pickMusicList.toArray());
            }
        }
       return  isDeleted;
    }

    @Override
    public void topPickMusic(Music music) {
        List<Music> newPickMusicList = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList();
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
        musicPickRepository.reset();
        musicPickRepository.rightPushAll(pickMusicList.toArray());
    }

    @Override
    public Long black(String id) {
        return musicBlackRepository.add(id);
    }

    @Override
    public Long unblack(String id) {
        return musicBlackRepository.remove(id);
    }

    @Override
    public boolean isBlack(String id) {
        return musicBlackRepository.isMember(id);
    }

    @Override
    public boolean isPicked(String id) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList();
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return true;
            }
        }
        Music playing = musicPlayingRepository.getPlaying();
        return playing.getId().equals(id);
    }

    public Object[] getMusicById(String id) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList();
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
            return searchQQ(music,hulkPage);
        }else if(music.getSource().equals("mg")){
            return searchMG(music,hulkPage);
        }else if(music.getSource().equals("lz")){
            return searchLZ(music,hulkPage);
        }
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/netease/songs/")
                .append(music.getName())
                .append("/search")
                .append("/").append(hulkPage.getPageIndex() - 1)
                .append("/").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 1) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("data");
                Integer count = responseJsonObject.getJSONObject("data").getInteger("count");
                List list = JSONObject.parseObject(JSONObject.toJSONString(data), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    private HulkPage searchQQ(Music music,HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/search?key=")
                .append(music.getName())
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
                .append("/search?keyword=")
                .append(music.getName())
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
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    @Override
    public boolean clearPlayList() {
        musicPickRepository.reset();
        return true;
    }

    @Override
    public String showBlackMusic() {
        Set blackList = musicBlackRepository.showBlackList();
        if(blackList != null && blackList.size() > 0){
           return String.join(",",blackList);
        }
        return  null;
    }

}

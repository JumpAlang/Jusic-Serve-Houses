package com.scoder.jusic.service;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;

import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
public interface MusicService {

    /**
     * 接收点歌请求，推送点歌信息
     *
     * @param sessionId session id
     * @param request   music info
     * @return music info
     */
    Music toPick(String sessionId, Music request,String houseId);

    /**
     * 切歌
     *
     * @return 将要播放的音乐
     */
    Music musicSwitch(String houseId);

    /**
     * get pick list
     *
     * @return linked list
     */
    LinkedList<Music> getPickList(String houseId);

    List<Music> getPickListNoPlaying(String houseId);

    LinkedList<Music> getSortedPickList(List<Music> musicList,String houseId);

    Music getPlaying(String houseId);
    /**
     * 修改点歌列表顺序
     *
     * @param musicList -
     * @return -
     */
    Long modifyPickOrder(LinkedList<Music> musicList,String houseId);

    /**
     * 投票
     *
     * @param sessionId session id
     * @return 0：投票失败，已经参与过。1：投票成功
     */
    Long vote(String sessionId,String houseId);

    /**
     * 从集合中获取参与投票的人数
     *
     * @return 参与投票的人数
     */
    Long getVoteCount(String houseId);

    /**
     * get music
     *
     * @param keyword keyword
     * @return music
     */
    Music getMusic(String keyword);

    Music getQQMusic(String keyword);
    Music getWYMusic(String keyword);

    Music getLZMusic(Integer index);

    Music getWYMusicById(String id);

    Music getQQMusicById(String id);

    Music getMGMusic(String keyword);

    Music getMGMusicById(String id);

    /**
     * get music url
     *
     * @param musicId music id
     * @return url
     */
    String getMusicUrl(String musicId);

    String getQQMusicUrl(String musicId);

    String getMGMusicUrl(String musicId,String musicName);

    /**
     * 删除音乐
     *
     * @param music music
     */
    boolean deletePickMusic(Music music,String houseId);
    /**
     * top pick music
     *
     * @param music -
     */
    void topPickMusic(Music music,String houseId);

    /**
     * black
     *
     * @param id music id
     * @return -
     */
    Long black(String id,String houseId);

    /**
     * un black
     *
     * @param id music id
     * @return -
     */
    Long unblack(String id,String houseId);

    /**
     * is black?
     *
     * @param id music id
     * @return -
     */
    boolean isBlack(String id,String houseId);

    /**
     * is picked ?
     *
     * @param id music id
     * @return
     */
    boolean isPicked(String id,String houseId);

    Object[] getMusicById(String id,String houseId);

        /**
         * search music
         * @param music music
         * @param hulkPage page
         * @return list
         */
    Page<List<Music>> search(Music music, HulkPage hulkPage);

    boolean clearPlayList(String houseId);

    String showBlackMusic(String houseId);

    Page<List<SongList>> search(SongList songList, HulkPage hulkPage);

    Page<List<MusicUser>> search(MusicUser musicUser, HulkPage hulkPage);

    boolean clearDefaultPlayList(String houseId);

    Integer addDefaultPlayList(String houseId,String[] playlistIds,String source);

    Long playlistSize(String houseId);

    void updateMusicUrl(Music result);

    }

package com.scoder.jusic.job;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;


/**
 * @author H
 */
@Component
@Slf4j
public class MusicJob {

    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;

    /**
     * 广播条件：第一次启动时 playing 为空、音乐播放完毕、投票切歌
     */
    @Scheduled(fixedRate = 1000)
    private void sendIfSufficient() {
        if (this.isPlayingNull()) {
            configRepository.setPushSwitch(true);
            log.info("推送开关开启, 原因: 首次启动");
        } else if (this.isPlayingOver()) {
            configRepository.setPushSwitch(true);
            log.info("推送开关开启, 原因: 上一首播放完毕");
        } else if (this.isPlayingSkip()) {
            configRepository.setPushSwitch(true);
            log.info("推送开关开启, 原因: 投票通过");
        }

        if (this.isPushSwitchOpen()) {
            log.info("检测到推送开关已开启");
            Music music = musicService.musicSwitch();
            long pushTime = System.currentTimeMillis();
            Long duration = music.getDuration() == null?300000L:music.getDuration();

            configRepository.setLastMusicPushTimeAndDuration(pushTime, duration);
            music.setPushTime(pushTime);
            musicPlayingRepository.leftPush(music);
            musicPlayingRepository.keepTheOne();
            log.info("已保存推送时间和音乐时长");
            configRepository.setPushSwitch(false);
            log.info("已关闭音乐推送开关");
            musicVoteRepository.reset();
            log.info("已重置投票");
            sessionService.send(MessageType.MUSIC, Response.success(music, "正在播放"));
            log.info("已向所有客户端推送音乐, 音乐: {}, 时长: {}, 推送时间: {}, 链接: {}", music.getName(), duration, pushTime, music.getUrl());
            LinkedList<Music> result = musicService.getPickList();
//            if(isGoodModel()){
//                sessionService.send(MessageType.PICK, Response.success(result, "goodlist"));
//            }else{
//                sessionService.send(MessageType.PICK, Response.success(result, "播放列表"));
//            }
            sessionService.send(MessageType.PICK, Response.success(result, "播放列表"));
            log.info("已向客户端推送播放列表, 共 {} 首, 列表: {}", result.size(), result);
        }
    }

    private boolean isPushSwitchOpen() {
        Boolean pushSwitch = configRepository.getPushSwitch();
        return pushSwitch == null ? false : pushSwitch;
    }

    private boolean isGoodModel(){
        Boolean goodModel = configRepository.getGoodModel();
        return goodModel == null ? false:goodModel;
    }

    private boolean isPlayingSkip() {
        Long voteSize = musicVoteRepository.size();
        Long sessionSize = sessionRepository.size();
        Float voteRate = configRepository.getVoteRate();
        if (voteSize != null && voteSize != 0 && sessionSize != null && voteRate != null) {
            return voteSize >= sessionSize * voteRate;
        }
        return false;
    }

    private boolean isPlayingOver() {
        Long lastMusicDuration = configRepository.getLastMusicDuration();
        Long lastMusicPushTime = configRepository.getLastMusicPushTime();
        if (null != lastMusicDuration && null != lastMusicPushTime) {
            return (lastMusicPushTime + lastMusicDuration) - System.currentTimeMillis() <= 0;
        }
        return false;
    }

    private boolean isPlayingNull() {
        return null == musicPlayingRepository.getPlaying();
    }

}

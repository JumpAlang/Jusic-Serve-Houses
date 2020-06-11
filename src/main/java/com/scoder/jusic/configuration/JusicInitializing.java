package com.scoder.jusic.configuration;

import com.scoder.jusic.repository.*;
import com.scoder.jusic.job.MusicTopJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicInitializing implements InitializingBean {

    private final JusicProperties jusicProperties;
    private final ResourceLoader resourceLoader;
    private final ConfigRepository configRepository;
    private final SessionRepository sessionRepository;
    private final MusicDefaultRepository musicDefaultRepository;
    private final MusicPlayingRepository musicPlayingRepository;
    private final MusicPickRepository musicPickRepository;
    private final MusicVoteRepository musicVoteRepository;
    private final SessionBlackRepository sessionBlackRepository;
    private final MusicBlackRepository musicBlackRepository;

    @Autowired
    private MusicTopJob musicTopJob;


    public JusicInitializing(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository, JusicProperties jusicProperties, ResourceLoader resourceLoader, SessionBlackRepository sessionBlackRepository,MusicBlackRepository musicBlackRepository) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.musicDefaultRepository = musicDefaultRepository;
        this.musicPlayingRepository = musicPlayingRepository;
        this.musicPickRepository = musicPickRepository;
        this.musicVoteRepository = musicVoteRepository;
        this.jusicProperties = jusicProperties;
        this.resourceLoader = resourceLoader;
        this.sessionBlackRepository = sessionBlackRepository;
        this.musicBlackRepository = musicBlackRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        clearSurvive();
        initialize();
    }

    /**
     * 读取默认列表
     *
     * @throws IOException -
     */
    private void initDefaultMusicId() throws IOException {
        try{
            ArrayList<String> musicList = musicTopJob.getMusicTop();
            if(musicList == null || musicList.size() == 0){
                InputStream inputStream = resourceLoader.getResource(jusicProperties.getDefaultMusicFile()).getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String musicId = "";
                // 逐行读取
                while ((musicId = bufferedReader.readLine()) != null) {
                    musicList.add(musicId);
                }
                musicList.add("512359558");
                musicList.add("316686");
                musicList.add("25718007");
            }
            JusicProperties.setDefaultListByJob(musicList);
        }catch (Exception e){

        }
    }

    /**
     * 初始化 config
     * 初始化 default
     */
    private void initialize() throws IOException {
        log.info("初始化工作开始");
        this.initDefaultMusicId();
        configRepository.initialize(JusicProperties.HOUSE_DEFAULT_ID);
        musicDefaultRepository.initialize("");
        log.info("初始化工作完成");
    }

    /**
     * 清理 session
     * 清理 config
     * 清理 default
     * 清理 playing
     * 清理 pick
     */
    private void clearSurvive() {
        log.info("清理工作开始");
        sessionRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        sessionBlackRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        configRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        musicDefaultRepository.destroy("");
        musicPlayingRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        musicPickRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        musicVoteRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        musicBlackRepository.destroy(JusicProperties.HOUSE_DEFAULT_ID);
        log.info("清理工作完成");
    }
}

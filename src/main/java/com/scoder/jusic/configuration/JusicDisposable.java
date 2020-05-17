package com.scoder.jusic.configuration;

import com.scoder.jusic.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicDisposable implements DisposableBean {

    private final ConfigRepository configRepository;
    private final SessionRepository sessionRepository;
    private final MusicDefaultRepository musicDefaultRepository;
    private final MusicPlayingRepository musicPlayingRepository;
    private final MusicPickRepository musicPickRepository;
    private final MusicVoteRepository musicVoteRepository;

    public JusicDisposable(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.musicDefaultRepository = musicDefaultRepository;
        this.musicPlayingRepository = musicPlayingRepository;
        this.musicPickRepository = musicPickRepository;
        this.musicVoteRepository = musicVoteRepository;
    }

    @Override
    public void destroy() throws Exception {
        log.info("销毁工作开始");
        sessionRepository.destroy();
        configRepository.destroy();
        musicDefaultRepository.destroy();
        musicPlayingRepository.destroy();
        musicPickRepository.destroy();
        musicVoteRepository.destroy();
        log.info("销毁工作完成");
    }

}

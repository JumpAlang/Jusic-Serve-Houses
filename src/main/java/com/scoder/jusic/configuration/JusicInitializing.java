package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicInitializing implements InitializingBean {

    private final JusicProperties jusicProperties;
    private final ResourceLoader resourceLoader;
    private final HouseContainer houseContainer;
    private final LifecycleProperties lifecycleProperties;

    @Autowired
    private MusicService musicService;

    @Autowired
    private ConfigService configService;


    public JusicInitializing(JusicProperties jusicProperties,
                             ResourceLoader resourceLoader,
                             HouseContainer houseContainer,
                             LifecycleProperties lifecycleProperties) {
        this.jusicProperties = jusicProperties;
        this.resourceLoader = resourceLoader;
        this.houseContainer = houseContainer;
        this.lifecycleProperties = lifecycleProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        CopyOnWriteArrayList<House> houses;
        if (lifecycleProperties.isCleanupLegacyStateOnStartup()) {
            log.warn("legacy startup cleanup is enabled; do not use this mode with multiple nodes");
            houses = houseContainer.clearSurvive();
        } else {
            houses = houseContainer.loadSurvive();
        }
        initialize(houses);
    }

    /**
     * 读取默认列表
     *
     * @throws IOException -
     */
    private void initDefaultMusicId() {
        // 节点启动不能依赖外部音乐源。在线榜单由 MusicTopJob 在启动后按计划刷新。
        ArrayList<String> musicList = new ArrayList<>();
        String path = "file:" + System.getProperty("user.dir")
                + File.separator + "default.txt";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resourceLoader.getResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String musicId;
            while ((musicId = reader.readLine()) != null) {
                String normalized = musicId.trim();
                if (!normalized.isEmpty()) {
                    musicList.add(normalized);
                }
            }
        } catch (IOException ex) {
            log.warn("Unable to read the local default playlist {}; using built-in defaults: {}",
                    path, ex.getMessage());
            log.debug("Local default playlist failure", ex);
        }
        if (musicList.isEmpty()) {
            musicList.add("512359558");
            musicList.add("316686");
            musicList.add("25718007");
        }
        JusicProperties.setDefaultListByJob(musicList);
    }

    /**
     * 初始化 config
     * 初始化 default
     */
    private void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        log.info("初始化工作开始");
        this.initDefaultMusicId();
        houseContainer.initialize(houses);
//        musicService.netEaseAutoLogin();
        try {
            musicService.neteaseRefreshCookie();
        } catch (RuntimeException ex) {
            // Provider availability must not decide whether the node can
            // start. The scheduled refresh retains the same bounded retry
            // path after startup.
            log.warn("网易云 Cookie 启动刷新失败，节点继续启动: {}", ex.getMessage());
            log.debug("网易云 Cookie 启动刷新异常", ex);
        }
        log.info("初始化工作完成");
//        configService.setQqMusicCookieToProperties();
        log.info("初始化qq cookie");
    }


}

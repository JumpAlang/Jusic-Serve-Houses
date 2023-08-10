package com.scoder.jusic.configuration;

import com.scoder.jusic.job.MusicTopJob;
import com.scoder.jusic.model.House;
import com.scoder.jusic.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
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

    @Autowired
    private MusicTopJob musicTopJob;

    @Autowired
    private MusicService musicService;


    public JusicInitializing(JusicProperties jusicProperties, ResourceLoader resourceLoader,HouseContainer houseContainer) {
        this.jusicProperties = jusicProperties;
        this.resourceLoader = resourceLoader;
        this.houseContainer = houseContainer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initialize(houseContainer.clearSurvive());
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
                InputStream inputStream = resourceLoader.getResource("file:"+System.getProperty("user.dir")+File.separator+"default.txt").getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String musicId = "";
                // 逐行读取
                while ((musicId = bufferedReader.readLine()) != null) {
                    musicList.add(musicId);
                }
            }
            if(musicList == null || musicList.isEmpty()){
                musicList = new ArrayList<>();
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
    private void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        log.info("初始化工作开始");
        this.initDefaultMusicId();
        houseContainer.initialize(houses);
        musicService.netEaseAutoLogin();
        log.info("初始化工作完成");
    }


}

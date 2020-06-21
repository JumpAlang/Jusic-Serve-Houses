package com.scoder.jusic.configuration;

import com.scoder.jusic.job.MusicTopJob;
import com.scoder.jusic.model.House;
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
    private void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        log.info("初始化工作开始");
        this.initDefaultMusicId();
        houseContainer.initialize(houses);
        log.info("初始化工作完成");
    }


}

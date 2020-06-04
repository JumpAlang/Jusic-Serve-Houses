package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.*;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author alang
 * @create 2020-05-21 2:28
 */
@Controller
@Slf4j
public class HouseController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ConfigRepository configRepository;

    @MessageMapping("/house/add")
    public void addHouse(House house, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if(house.getName() == null || house.getName() == ""){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "房间名称不能为空"),houseId);
            return;
        }
        if(houseContainer.contains(sessionId)){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "你已经创建过一个房间，待其被自动腾空方可再创建"),houseId);
            return;
        }
        if(houseContainer.size() >= jusicProperties.getHouseSize()){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "暂时不能新增房间，待其他空房间被自动腾空方可创建。"),houseId);
            return;
        }
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
        house.setEnableStatus(true);
        house.setPassword(sessionId);
        house.setSessionId(sessionId);
        house.setRemoteAddress((String)(accessor.getSessionAttributes().get("remoteAddress")));//IPUtils.getRemoteAddress(request);
        houseContainer.add(house);
        log.info("house添加成功"+house.getName());
        WebSocketSession oldSession = sessionService.clearSession(sessionId,houseId);
        oldSession.getAttributes().put("houseId",sessionId);
        sessionService.putSession(oldSession,sessionId);
        //通知当前要离开的房间总数变化，及推送最新房间歌单等

        // 2. send playing
//        try {
//            Thread.sleep(1100);//睡一下，让定时任务先选歌
//        } catch (InterruptedException e) {
//            log.error(e.getMessage());
//        }
//        if(configRepository.getLastMusicPushTime(sessionId) == null){
//            configRepository.setPushSwitch(false,sessionId);
//            Music music = musicService.musicSwitch(sessionId);
//            long pushTime = System.currentTimeMillis();
//            Long duration = music.getDuration() == null?300000L:music.getDuration();
//            configRepository.setLastMusicPushTimeAndDuration(pushTime, duration,sessionId);
//            music.setPushTime(pushTime);
//            musicPlayingRepository.leftPush(music,sessionId);
//            musicPlayingRepository.keepTheOne(sessionId);
//            log.info("已保存推送时间和音乐时长"+house.getName());
//            log.info("已关闭音乐推送开关"+house.getName());
//            sessionService.send(MessageType.MUSIC, Response.success(music, "正在播放"),sessionId);
//            log.info("已向所有客户端推送音乐, 音乐: {}, 时长: {}, 推送时间: {}, 链接: {}", music.getName(), duration, pushTime, music.getUrl());
//            LinkedList<Music> result = musicService.getPickList(sessionId);
//            sessionService.send(MessageType.PICK, Response.success(result, "播放列表"),sessionId);
//
//        }
      // 1. send online
        Online online = new Online();
        online.setCount(jusicProperties.getSessions(sessionId).size());
        sessionService.send(MessageType.ONLINE, Response.success(online),sessionId);
        // 4.设置当前用户为管理员
        sessionService.send(oldSession,
                MessageType.AUTH_ADMIN,
                 Response.success((Object) null, "创建房间成功"));
        //设置默认为点赞模式
        sessionService.send(MessageType.GOODMODEL, Response.success("GOOD", "goodlist"),sessionId);

        // 5.设置要离开的房间总人数
        int oldHouseCount = jusicProperties.getSessions(houseId).size();
        online.setCount(oldHouseCount);
        if(oldHouseCount == 0 && !houseId.equals(JusicProperties.HOUSE_DEFAULT_ID)){
            houseContainer.destroy(houseId);
        }else{
            sessionService.send(MessageType.ONLINE, Response.success(online),houseId);
        }
        sessionService.send(oldSession,
                MessageType.ADD_HOUSE,
                 Response.success((Object) null, "创建房间成功"));
    }
    @MessageMapping("/house/enter/{enterHouseId}")
    public void enterHouse(@DestinationVariable String enterHouseId, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if(houseId.equals(enterHouseId)){
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.success((Object) null, "进入房间成功"),houseId);
            return;
        }
        if(!houseContainer.contains(enterHouseId)){
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.failure((Object) null, "房间已经不存在"),houseId);
            return;
        }
        WebSocketSession oldSession = sessionService.clearSession(sessionId,houseId);
        oldSession.getAttributes().put("houseId",enterHouseId);
        User user = sessionService.putSession(oldSession,enterHouseId);
        //通知当前要离开的房间总数变化，及推送最新房间歌单等
        // 1. send online
        Online online = new Online();
        online.setCount(jusicProperties.getSessions(enterHouseId).size());
        sessionService.send(MessageType.ONLINE, Response.success(online),enterHouseId);
        // 2. send playing
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            log.error(e.getMessage());
//        }
        Music playing = musicPlayingRepository.getPlaying(enterHouseId);
        sessionService.send(oldSession, MessageType.MUSIC, Response.success(playing, "正在播放"));
        // 3. send pick list
        LinkedList<Music> pickList = musicService.getPickList(enterHouseId);
        if(configService.getGoodModel(enterHouseId) != null && configService.getGoodModel(enterHouseId)) {
            sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "goodlist"));
        }else{
            sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "播放列表"));
        }
        // 4.设置当前用户角色
        if(user.getRole() == "admin"){
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.success((Object) null, "切换房间成功且登录成功"));
        }else{
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.failure((Object) null, "切换房间成功"));
        }
        // 5.设置要离开的房间总人数
        int oldHouseCount = jusicProperties.getSessions(houseId).size();
        online.setCount(oldHouseCount);
        if(oldHouseCount == 0 && !houseId.equals(JusicProperties.HOUSE_DEFAULT_ID)){
            houseContainer.destroy(houseId);
        }else{
            sessionService.send(MessageType.ONLINE, Response.success(online),houseId);
        }
        sessionService.send(oldSession,
                MessageType.ENTER_HOUSE,
                Response.success((Object) null, "进入房间成功"));
    }


    @MessageMapping("/house/search")
    public void searchHouse(StompHeaderAccessor accessor) {
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        ArrayList<House> housesSimple = new ArrayList<>();
        for(House house : houses){
            House houseSimple = new House();
            houseSimple.setName(house.getName());
            houseSimple.setId(house.getId());
            houseSimple.setDesc(house.getDesc());
            houseSimple.setCreateTime(house.getCreateTime());
            housesSimple.add(houseSimple);
        }
        String sessionId = accessor.getHeader("simpSessionId").toString();
        sessionService.send(sessionId, MessageType.SEARCH_HOUSE, Response.success(housesSimple, "房间列表"),houseId);
    }

}

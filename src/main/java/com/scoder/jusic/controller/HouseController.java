package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.ConnectionAdmissionException;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.configuration.SessionCommandLocks;
import com.scoder.jusic.handler.JusicWebSocketHandlerAsync;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.model.*;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.security.RoomRolePolicy;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

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
    @Autowired
    private HouseOperationLocks houseOperationLocks;
    @Autowired
    private SessionCommandLocks sessionCommandLocks;
    @Autowired
    private RoomMusicLocks roomMusicLocks;
    @Autowired(required = false)
    private MusicJob musicJob;

    @MessageMapping("/house/add")
    public void addHouse(House house, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        ReentrantLock commandLock = sessionCommandLocks.getLock(sessionId);
        commandLock.lock();
        try {
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if(house.getName() == null || house.getName() == "" || house.getName().length() > 33){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "房间名称不能为空且不能超过33个字符"),houseId);
            return;
        }
        if(house.getDesc() != null && house.getDesc().length() > 133){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "房间描述不能超过133个字符"),houseId);
            return;
        }
        if(house.getNeedPwd() != null && house.getNeedPwd()){
            if(house.getPassword() == null || "".equals(house.getPassword().trim())){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "房间密码不能为空"),houseId);
                return;
            }else if(StringUtils.isUrlSpecialCharacter(house.getPassword())){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "密码不能有如下字符：空格、?、%、#、&、=、+"),houseId);
                return;
            }
            house.setPassword(house.getPassword().trim());
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
        String ip = (String)(accessor.getSessionAttributes().get("remoteAddress"));
        if(houseContainer.isBeyondIpHouse(ip,jusicProperties.getIpHouse())){
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "该网络暂时不能新增房间，待其他空房间被自动腾空方可创建。"),houseId);
            return;}
        if(house.getEnableStatus() != null && house.getEnableStatus()){
            if(house.getRetainKey() == null || "".equals(house.getRetainKey().trim())){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "订单号不能为空。"),houseId);
                return;
            }
            RetainKey key = houseContainer.getRetainKey(house.getRetainKey());
            if(key == null){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "订单号不存在或须等待3分钟后系统才能生效，如3分钟后还不存在，请加q群：672905926。"),houseId);
                return;
            }else if(key.getIsUsed()){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "订单号已被使用"),houseId);
                return;

            }else if(key.getExpireTime() != null && key.getExpireTime() < System.currentTimeMillis()){
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "订单号已过期"),houseId);
                return;
            }
            key.setHouseId(sessionId);
            key.setRemoteAddress(ip);
            key.setUsedTime(System.currentTimeMillis());
            key.setIsUsed(true);
            houseContainer.updateRetainKey(key);
        }
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
        house.setSessionId(sessionId);
        house.setRemoteAddress(ip);//IPUtils.getRemoteAddress(request);
        house.setAdminPwd(jusicProperties.getRoleAdminPassword());
        WebSocketSession oldSession = jusicProperties.getSessionIfPresent(houseId, sessionId);
        LinkedList<Music> previousPickList = null;
        String previousRole;
        try (HouseOperationLocks.LockScope ignored =
                     houseOperationLocks.lockBoth(houseId, sessionId)) {
            if (!isConnectionActive(oldSession)) {
                return;
            }
            // Recheck under the same lifecycle lock used by add/destroy so a
            // duplicate create cannot silently move this connection into a
            // room created by another request.
            if (houseContainer.contains(sessionId)) {
                sessionService.send(oldSession, MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "你已经创建过一个房间，待其被自动腾空方可再创建"));
                return;
            }
            try {
                previousRole = sessionService.getRole(sessionId, houseId);
                houseContainer.add(house);
                synchronized (oldSession) {
                    if (!isConnectionActive(oldSession)) {
                        houseContainer.destroy(sessionId);
                        return;
                    }
                    Object currentHouseId = oldSession.getAttributes().get("houseId");
                    if (currentHouseId == null
                            || !houseId.equals(currentHouseId.toString())) {
                        houseContainer.destroy(sessionId);
                        sessionService.send(oldSession, MessageType.ADD_HOUSE,
                                Response.failure((Object) null,
                                        "连接状态已变化，请刷新页面后重试"));
                        return;
                    }
                    // SessionService performs one atomic local room move and
                    // only then reconciles Redis; there is no remove/add gap.
                    sessionService.putSession(oldSession,sessionId);
                }
            } catch (ConnectionAdmissionException ex) {
                houseContainer.destroy(sessionId);
                sessionService.send(oldSession, MessageType.ADD_HOUSE,
                        Response.failure((Object) null, admissionFailureMessage(ex)));
                return;
            } catch (RuntimeException ex) {
                houseContainer.destroy(sessionId);
                log.error("Failed to move session {} into newly-created house {}",
                        sessionId, sessionId, ex);
                sessionService.send(oldSession, MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "房间创建失败，请稍后重试"));
                return;
            }
            previousPickList = cleanupQueuedMusicAfterRoomSwitch(
                    sessionId, houseId, previousRole);
        }
        // Send transition markers only after the atomic move. putSession drops
        // frames queued for the old room, so sending this earlier could lose it.
        sessionService.send(oldSession,
                MessageType.ADD_HOUSE_START,
                Response.success((Object)null, "房间创建开始"));
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
        online.setCount(jusicProperties.getSessionCount(sessionId));
        sessionService.send(MessageType.ONLINE, Response.success(online),sessionId);

        int oldHouseCount = jusicProperties.getSessionCount(houseId);
        online.setCount(oldHouseCount);
        if(oldHouseCount != 0){
            sessionService.send(MessageType.ONLINE, Response.success(online),houseId);
            if (previousPickList != null) {
                sessionService.send(MessageType.PICK,
                        Response.success(previousPickList, "点歌列表"), houseId);
            }
        }
        // 4.设置当前用户为管理员
        sessionService.send(oldSession,
                MessageType.AUTH_ADMIN,
                 Response.success((Object) null, "您是本房间的主人"));
        //设置默认为点赞模式
        sessionService.send(MessageType.GOODMODEL, Response.success("GOOD", "goodlist"),sessionId);

        // 5.设置要离开的房间总人数
        if(oldHouseCount == 0){
            houseContainer.destroyIfTemporaryAndEmpty(houseId);
        }
        sessionService.send(oldSession,
                MessageType.ADD_HOUSE,
                 Response.success(sessionId, "创建房间成功"));
        sessionService.send(oldSession, MessageType.ROOM_STATE,
                Response.success(RoomState.read(configService, sessionId)));
        } finally {
            commandLock.unlock();
        }
    }
    @MessageMapping("/house/enter")
    public void enterHouse(House house, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        ReentrantLock commandLock = sessionCommandLocks.getLock(sessionId);
        commandLock.lock();
        try {
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if(houseId.equals(house.getId())){
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.success((Object) null, "进入房间成功"),houseId);
            return;
        }
        String targetHouseId = house.getId();
        WebSocketSession oldSession = jusicProperties.getSessionIfPresent(houseId, sessionId);
        House matchHouse;
        User user;
        String previousHouseId;
        String previousRole;
        LinkedList<Music> previousPickList = null;
        try (HouseOperationLocks.LockScope ignored =
                     houseOperationLocks.lockBoth(houseId, targetHouseId)) {
            // Existence and password validation must be repeated while room
            // destruction is excluded by the target lifecycle lock.
            matchHouse = houseContainer.get(targetHouseId);
            if(matchHouse == null){
                sessionService.send(sessionId,
                        MessageType.ENTER_HOUSE,
                        Response.failure((Object) null, "房间已经不存在"),houseId);
                return;
            }
            if(Boolean.TRUE.equals(matchHouse.getNeedPwd())
                    && !Objects.equals(matchHouse.getPassword(), house.getPassword())){
                sessionService.send(sessionId,
                        MessageType.ENTER_HOUSE,
                        Response.failure((Object) null, "请输入正确的房间密码"),houseId);
                return;
            }
            if (!isConnectionActive(oldSession)) {
                return;
            }
            try {
                // Capture the role before putSession removes the old-room
                // membership and creates the target-room default role.
                previousRole = sessionService.getRole(sessionId, houseId);
                synchronized (oldSession) {
                    if (!isConnectionActive(oldSession)) {
                        return;
                    }
                    Object currentHouseId = oldSession.getAttributes().get("houseId");
                    if (currentHouseId == null
                            || !houseId.equals(currentHouseId.toString())) {
                        sessionService.send(oldSession, MessageType.ENTER_HOUSE,
                                Response.failure((Object) null,
                                        "连接状态已变化，请刷新页面后重试"));
                        return;
                    }
                    previousHouseId = currentHouseId.toString();
                    user = sessionService.putSession(oldSession,targetHouseId);
                }
            } catch (ConnectionAdmissionException ex) {
                sessionService.send(oldSession, MessageType.ENTER_HOUSE,
                        Response.failure((Object) null, admissionFailureMessage(ex)));
                return;
            } catch (RuntimeException ex) {
                log.error("Failed to move session {} from house {} to {}",
                        sessionId, houseId, targetHouseId, ex);
                sessionService.send(oldSession, MessageType.ENTER_HOUSE,
                        Response.failure((Object) null, "切换房间失败，请稍后重试"));
                return;
            }

            previousPickList = cleanupQueuedMusicAfterRoomSwitch(
                    sessionId, previousHouseId, previousRole);
        }
        houseId = previousHouseId;
        sessionService.send(oldSession,
                MessageType.ENTER_HOUSE_START,
                Response.success((Object) null, "进入房间开始"));

        // 1. send online
        Online online = new Online();
        online.setCount(jusicProperties.getSessionCount(house.getId()));
        sessionService.send(MessageType.ONLINE, Response.success(online),house.getId());

        //通知当前要离开的房间总数变化，及推送最新房间歌单等
        int oldHouseCount = jusicProperties.getSessionCount(houseId);
        online.setCount(oldHouseCount);
        if(oldHouseCount != 0){
            sessionService.send(MessageType.ONLINE, Response.success(online),houseId);
            if (previousPickList != null) {
                sessionService.send(MessageType.PICK,
                        Response.success(previousPickList, "点歌列表"), houseId);
            }
        }
        // 2. send playing
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            log.error(e.getMessage());
//        }
        Music playing = musicPlayingRepository.getPlaying(house.getId());
        boolean isOver = playing == null || (musicJob != null && musicJob.isPlayingOver(house.getId()));
        if (playing != null && !isOver) {
            try {
                musicService.updateMusicUrl(playing);
            } catch (RuntimeException ex) {
                // Joining the room must not fail after membership has already
                // moved just because a provider URL refresh is unavailable.
                log.warn("房间 {} 的播放链接刷新失败，继续发送现有快照: {}",
                        house.getId(), ex.getMessage());
                log.debug("进入房间时播放链接刷新异常", ex);
            }
            playing.setIps(null);
            sessionService.send(oldSession, MessageType.MUSIC, Response.success(playing, "正在播放"));
        } else {
            if (configRepository != null) {
                configRepository.setPushSwitch(true, house.getId());
            }
            if (musicJob != null) {
                musicJob.triggerHouse(house.getId());
            }
        }
        // 3. Always replace the previous room's queue/config snapshot. A new
        // or currently idle room has no playing record yet, but the browser
        // must still clear the old room's queue and good-mode state.
        LinkedList<Music> pickList = musicService.getPickList(house.getId());
        Boolean goodModel = configService.getGoodModel(house.getId());
        if(Boolean.TRUE.equals(goodModel)) {
            sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "goodlist"));
            sessionService.send(oldSession,MessageType.GOODMODEL, Response.success("GOOD", "goodlist"));
        }else{
            sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "播放列表"));
            sessionService.send(oldSession,MessageType.GOODMODEL, Response.success("EXITGOOD", "goodlist"));
        }
        // 4.设置当前用户角色
        if("admin".equals(user.getRole())){
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.success((Object) null, "欢迎主人"));
        }else{
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.failure((Object) null, "切换房间成功"));
        }
        // 5.设置要离开的房间总人数
        if(oldHouseCount == 0){
            houseContainer.destroyIfTemporaryAndEmpty(houseId);
        }
        if(matchHouse.getAnnounce() != null && matchHouse.getAnnounce().getContent() != null && !"".equals(matchHouse.getAnnounce().getContent().trim())){
            sessionService.send(oldSession,MessageType.ANNOUNCEMENT, Response.success(matchHouse.getAnnounce(), "房间公告"));
        }else{
            sessionService.send(oldSession,MessageType.ANNOUNCEMENT, Response.success("", "房间公告"));
        }
        sessionService.send(oldSession,
                MessageType.ENTER_HOUSE,
                Response.success(house.getId(), "进入房间成功"));
        sessionService.send(oldSession, MessageType.ROOM_STATE,
                Response.success(RoomState.read(configService, house.getId())));
        } finally {
            commandLock.unlock();
        }
    }

    private boolean isConnectionActive(WebSocketSession session) {
        return session != null
                && session.isOpen()
                && !Boolean.TRUE.equals(session.getAttributes().get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE));
    }

    private boolean shouldClearQueuedMusicOnRoomSwitch(String role) {
        return !RoomRolePolicy.isPrivileged(role)
                && !RoomRolePolicy.hasCapability(role, "picker");
    }

    /** Caller must already hold the room lifecycle lock. */
    private LinkedList<Music> cleanupQueuedMusicAfterRoomSwitch(
            String sessionId, String previousHouseId, String previousRole) {
        if (!shouldClearQueuedMusicOnRoomSwitch(previousRole)) {
            return null;
        }

        // Global lock order is lifecycle -> room music. Exact Redis removals
        // never overwrite unrelated concurrent picks through reset/rewrite.
        ReentrantLock musicLock = roomMusicLocks.getLock(previousHouseId);
        musicLock.lock();
        try {
            long removed = musicService.removeQueuedMusicBySession(
                    sessionId, previousHouseId);
            return removed > 0L ? musicService.getPickList(previousHouseId) : null;
        } catch (RuntimeException ex) {
            // The room move has already committed. A cleanup outage must not
            // report a false switch failure or roll the membership back.
            log.warn("用户 {} 切离房间 {} 后清理待播歌曲失败: {}",
                    sessionId, previousHouseId, ex.getMessage());
            log.debug("切换房间后的待播歌曲清理异常", ex);
            return null;
        } finally {
            musicLock.unlock();
        }
    }

    private String admissionFailureMessage(ConnectionAdmissionException exception) {
        switch (exception.getReason()) {
            case ROOM_LIMIT:
                return "房间人数已达上限，请稍后重试";
            case IP_LIMIT:
                return "当前网络连接数已达上限，请稍后重试";
            case NODE_LIMIT:
                return "服务器连接数已满，请稍后重试";
            case SESSION_ID_CONFLICT:
            default:
                return "连接状态冲突，请刷新页面后重试";
        }
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
            houseSimple.setNeedPwd(house.getNeedPwd());
            houseSimple.setPopulation(jusicProperties.getSessionCount(house.getId()));
            housesSimple.add(houseSimple);
        }
        String sessionId = accessor.getHeader("simpSessionId").toString();
        sessionService.send(sessionId, MessageType.SEARCH_HOUSE, Response.success(housesSimple, "房间列表"),houseId);
    }

    /**
     * 房间留存与否
     * @param accessor
     */
    @MessageMapping("/house/retain/{retain}")
    public void houseRetain(@DestinationVariable boolean retain, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!"root".equals(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            houseContainer.get(houseId).setEnableStatus(retain);
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "设置房间留存与否成功"),houseId);
        }
    }

    @MessageMapping("/house/houseuser")
    public void houseuser(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        List<User> users = sessionService.getSession(houseId);
        users.forEach(user ->{
            user.setRemoteAddress(StringUtils.desensitizeIPV4(user.getRemoteAddress()));
        });
        sessionService.send(sessionId, MessageType.HOUSE_USER, Response.success(users, "获取房间用户成功"),houseId);
    }
}

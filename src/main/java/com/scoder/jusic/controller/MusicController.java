package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.model.*;
import com.scoder.jusic.security.InboundPayloadValidator;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.security.RoomRolePolicy;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.RoomCommandService;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
@Controller
@Slf4j
public class MusicController {

    private static final int MAX_PICK_WAIT_MINUTES = 24 * 60;

    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicService musicService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private RoomCommandService roomCommandService;
    @Autowired
    private InboundPayloadValidator inboundPayloadValidator;
    @Autowired
    private ProviderCallExecutor providerCallExecutor;
    @Autowired
    private MusicJob musicJob;
    @MessageMapping("/music/pick")
    public void pick(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor == null ? null : accessor.getSessionId();
        String houseId = accessor == null || accessor.getSessionAttributes() == null
                ? null
                : (String) accessor.getSessionAttributes().get("houseId");
        if (sessionId == null || houseId == null) {
            return;
        }

        String validationMessage = inboundPayloadValidator.validatePick(music);
        if (validationMessage != null) {
            sendPickFailure(sessionId, houseId, validationMessage);
            return;
        }
        log.info("收到点歌请求，source: {}, idPresent: {}", music.getSource(), hasText(music.getId()));

        House matchHouse = houseContainer.get(houseId);
        if (matchHouse == null) {
            sendPickFailure(sessionId, houseId, "房间不存在");
            return;
        }
        String pickDenialMessage = pickDenialMessage(sessionId, houseId);
        if (pickDenialMessage != null) {
            sendPickFailure(sessionId, houseId, pickDenialMessage);
            return;
        }
        long limitPlaylistSize = playlistLimit(matchHouse);
        Long playlistSize = musicService.noDefaultPlaylistSize(houseId);
        if (playlistSize != null && playlistSize >= limitPlaylistSize) {
            sendPickFailure(sessionId, houseId, "最多能点播" + limitPlaylistSize + "首歌");
            return;
        }

        Music resolvedMusic;
        try {
            resolvedMusic = providerCallExecutor.pick(() -> resolveMusic(music));
        } catch (RuntimeException ex) {
            log.warn("点歌音乐解析失败，source: {}, reason: {}", music.getSource(), ex.getMessage());
            sendPickFailure(sessionId, houseId, "点歌失败,暂无此音乐或须单独购买");
            return;
        }
        if (resolvedMusic == null || !hasText(resolvedMusic.getUrl())) {
            log.info("点歌失败，音乐接口未返回可播放地址，source: {}", music.getSource());
            sendPickFailure(sessionId, houseId, "点歌失败,暂无此音乐或须单独购买");
            return;
        }

        PickMusicResult pickResult = roomCommandService.commitPick(
                sessionId, houseId, music.getSource(), resolvedMusic);
        if (!pickResult.isSuccess()) {
            sendPickFailure(sessionId, houseId, pickResult.getMessage());
            return;
        }

        Chat chat = new Chat();
        chat.setContent("点歌 " + music.getName());
        chat.setNickName(sessionService.getNickName(sessionId,houseId));
        chat.setSendTime(System.currentTimeMillis());
        chat.setSessionId(sessionId);
        sessionService.send(MessageType.CHAT, Response.success(chat),houseId);
        sessionService.send(MessageType.NOTICE, Response.success((Object) null, "点歌成功"),houseId);
        sessionService.send(MessageType.PICK,
                Response.success(pickResult.getPickList(), "点歌列表"),houseId);
        log.info("点歌成功，musicId: {}", pickResult.getMusic().getId());
    }

    private Music resolveMusic(Music music) {
        if ("qq".equals(music.getSource())) {
            return music.getId() != null
                    ? musicService.getQQMusicById(music.getId(), music.getQuality())
                    : musicService.getQQMusic(music);
        }
        if ("mg".equals(music.getSource())) {
            return music.getId() != null
                    ? musicService.getMGMusicById(music.getId())
                    : musicService.getMGMusic(music.getName());
        }
        if ("lz".equals(music.getSource())) {
            if (StringUtils.isQQMusicId(music.getId())) {
                return musicService.getQQMusicById(music.getId(), music.getQuality());
            }
            if (StringUtils.isWYMusicId(music.getId())) {
                return musicService.getWYMusicById(music.getId(), music.getQuality());
            }
            return musicService.getLZMusic(Integer.valueOf(music.getId()));
        }
        if ("ai".equals(music.getSource())) {
            if (StringUtils.isQQMusicId(music.getId())) {
                return musicService.getQQMusicById(music.getId(), music.getQuality());
            }
            if (StringUtils.isWYMusicId(music.getId())) {
                return musicService.getWYMusicById(music.getId(), music.getQuality());
            }
            return musicService.getAIMusic(music.getId());
        }
        if ("wydt".equals(music.getSource())) {
            return musicService.getWYDTMusicById(music.getId(), music.getQuality());
        }
        return music.getId() != null
                ? musicService.getWYMusicById(music.getId(), music.getQuality())
                : musicService.getWYMusic(music);
    }

    private String pickDenialMessage(String sessionId, String houseId) {
        String role = sessionService.getRole(sessionId,houseId);
        if (RoomRolePolicy.isPrivileged(role)) {
            return null;
        }
        if (Boolean.FALSE.equals(configService.getEnableSearch(houseId))
                && !RoomRolePolicy.hasCapability(role, "picker")) {
            return "当前禁止点歌";
        }
        Integer configuredMinutes = configService.getPickWaitMinutes(houseId);
        int waitMinutes = configuredMinutes == null ? 0 : Math.max(0, configuredMinutes);
        if (waitMinutes == 0) {
            return null;
        }
        User user = sessionService.getUser(sessionId, houseId);
        if (user == null || user.getJoinedAt() == null) {
            return null;
        }
        long waitMillis = waitMinutes * 60_000L;
        long remainingMillis = waitMillis - (System.currentTimeMillis() - user.getJoinedAt());
        if (remainingMillis <= 0) {
            return null;
        }
        long remainingMinutes = (remainingMillis + 59_999L) / 60_000L;
        return "进入房间满" + waitMinutes + "分钟后才能点歌，还需等待约"
                + remainingMinutes + "分钟";
    }

    private long playlistLimit(House house) {
        return Boolean.TRUE.equals(house.getEnableStatus())
                ? jusicProperties.getForeverlistSize()
                : jusicProperties.getPlaylistSize();
    }

    private void sendPickFailure(String sessionId, String houseId, String message) {
        sessionService.send(sessionId, MessageType.NOTICE,
                Response.failure((Object) null, message),houseId);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @MessageMapping("/music/good/{musicId}")
    public void good(@DestinationVariable String musicId, StompHeaderAccessor accessor) {
        // 点歌消息反馈
        String sessionId = accessor.getHeader("simpSessionId").toString();
        accessor.getSessionAttributes().get("remoteAddress");
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String nickName = sessionService.getNickName(sessionId,houseId);
        Chat chat = new Chat();
        if(configService.getGoodModel(houseId) == null || !configService.getGoodModel(houseId)) {
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "当前不是点赞模式"),houseId);
            return;
        }
        String ip = (String)(accessor.getSessionAttributes().get("remoteAddress"));//IPUtils.getRemoteAddress(request);
        Object[] musics = musicService.getMusicById(musicId,houseId);
        if(musics == null){
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "点歌列表未发现此歌"),houseId);
            return;
        }
        Music music = (Music)musics[0];
        HashSet<String> ips = music.getIps();
        if(ips == null || ips.size() == 0){
            ips = new HashSet<>();
            ips.add(ip);
            music.setGoodTime(System.currentTimeMillis());
            music.setIps(ips);
        }else{
            if(ips.contains(ip)){
                sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "已点赞过" + music.getName()+"，总票数"+ips.size()),houseId);
                return;
            }
            music.setGoodTime(System.currentTimeMillis());
            ips.add(ip);
        }
        chat.setContent(music.getName()+"点赞数"+ips.size());
        chat.setNickName(nickName);
        chat.setSessionId(sessionId);
        sessionService.send(MessageType.CHAT, Response.success(chat),houseId);
        // 点歌结果反馈
        LinkedList pickList = musicService.getSortedPickList((List<Music>)musics[1],houseId);
        sessionService.send(MessageType.PICK, Response.success(pickList, "点歌列表"),houseId);
    }

    @MessageMapping("/music/volumn/{volumn}")
    public void voice(@DestinationVariable Double volumn, StompHeaderAccessor accessor) {
        // 点歌消息反馈
        log.info("收到调整音量请求: {}", volumn);
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "调整音量成功"),houseId);
            sessionService.send(MessageType.VOLUMN, Response.success(volumn, "调整后的音量"),houseId);

        }
    }
    @MessageMapping("/music/vote/{voteRate}")
    public void voteRate(@DestinationVariable Float voteRate, StompHeaderAccessor accessor) {

        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (voteRate == null || voteRate > 1 || voteRate <= 0) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "投票率在(0,1]区间"),houseId);
            return;
        }
        if (!RoomRolePolicy.isPrivileged(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            configService.setVoteRate(voteRate,houseId);
            broadcastRoomState(houseId);
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "投票率被修改为"+voteRate),houseId);
        }
    }

    @MessageMapping("/music/skip/vote")
    public void skip(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (Boolean.FALSE.equals(configService.getEnableSwitch(houseId))
                && !RoomRolePolicy.isPrivileged(role)
                && !RoomRolePolicy.hasCapability(role, "voter")) {
            sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "禁止切歌"),houseId);
            return;
        }
        Long voteCount = 0L;
        Long vote = 0L;
        Integer size = jusicProperties.getSessionCount(houseId);
        if (RoomRolePolicy.isPrivileged(role)) {
            // 管理员
            configService.setPushSwitch(true,houseId);
            musicJob.wakeHouse(houseId);
            log.info("推送开关开启, 原因: 投票通过 - 管理员参与投票");
            sendAcceptedSkipChat(sessionId, houseId);
            sessionService.send(MessageType.NOTICE,
                    Response.success((Object) null, "已接受切歌请求"),houseId);
        } else {
            // 投票
            vote = musicService.vote(sessionId,houseId);
            voteCount = musicService.getVoteCount(houseId);
            if (vote == 0) {
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "你已经投过票了,当前状态："+voteCount + "/" + size),houseId);
                log.info("你已经投过票了");
                return;
            }else{
                sendAcceptedSkipChat(sessionId, houseId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, voteCount + "/" + size + " 投票成功"),houseId);
            }
        }
        log.info("投票成功");
        Float voteRate = configService.getVoteRate(houseId);
        if (vote != 0 && voteRate != null
                && voteCount >= size * voteRate) {
            // Do not leave a newly accepted skip behind backoff from an older
            // provider outage.
            configService.setPushSwitch(true, houseId);
            musicJob.wakeHouse(houseId);
        }
        if (voteCount == 1 && vote != 0 && voteCount < size * voteRate) {
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "有人希望切歌, 如果支持请发送“投票切歌”"),houseId);
            log.info("有人希望切歌, 如果支持请发送“投票切歌”");
    }
    }

    private void sendAcceptedSkipChat(String sessionId, String houseId) {
        Chat chat = new Chat();
        chat.setSessionId(sessionId);
        chat.setSendTime(System.currentTimeMillis());
        chat.setContent("投票切歌");
        chat.setNickName(sessionService.getNickName(sessionId,houseId));
        sessionService.send(MessageType.CHAT, Response.success(chat),houseId);
    }

    /**
     * 调整音乐顺序
     *
     * @return 调整后的点歌列表
     */
    @MessageMapping("/music/order")
    @SendTo("/topic/music/order")
    public Response modifyPickOrder(LinkedList<Music> musicList, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            return Response.failure((Object) null, "你没有权限");
        }

        musicService.modifyPickOrder(musicList,houseId);
        return Response.success(musicList, "调整成功");
    }

    @MessageMapping("/music/clear")
    public void clearPlayerList(StompHeaderAccessor accessor){
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if (!hasText(houseId)) {
            return;
        }
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            if (!musicService.clearPlayList(houseId)) {
                sessionService.send(sessionId, MessageType.NOTICE,
                        Response.failure((Object) null, "待播列表已为空或清空失败"), houseId);
                return;
            }
            LinkedList<Music> pickList = new LinkedList<>();
            Music music = musicService.getPlaying(houseId);
            if (music != null) {
                pickList.add(music);
            }
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "清空列表成功"),houseId);
            sessionService.send(MessageType.PICK, Response.success(pickList, "清空后的播放列表"),houseId);

        }
    }
    @MessageMapping("/music/clearDefaultPlayList")
    public void clearDefaultPlayerList(StompHeaderAccessor accessor){
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            musicService.clearDefaultPlayList(houseId);
            sessionService.send(MessageType.DEFAULT_PLAYLIST_CHANGED, Response.success(houseId), houseId);
            LinkedList<Music> pickList = new LinkedList<>();
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "清空列表成功"),houseId);
        }
    }
    /**
     * 删除播放列表的音乐
     *
     * @param music    music
     * @param accessor accessor
     */
    @MessageMapping("/music/delete")
    public void deletePickMusic(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if (!hasText(houseId)) {
            return;
        }
        if (music == null || !hasText(music.getId())) {
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "请输入完整歌曲信息"), houseId);
            return;
        }
        String role = sessionService.getRole(sessionId,houseId);
        music.setName(music.getId());
        if (!RoomRolePolicy.isPrivileged(role)) {
//            log.info("session: {} 尝试删除音乐但没有权限, 已被阻止", sessionId);
            music.setSessionId(sessionId);
//            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"));
        }
            if(musicService.deletePickMusic(music,houseId)){
                LinkedList<Music> pickList = musicService.getPickList(houseId);
                log.info("session: {} 删除音乐: {} 已成功, 即将广播删除后的播放列表", sessionId, music.getName());
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "删除成功"),houseId);
                sessionService.send(MessageType.PICK, Response.success(pickList, "删除后的播放列表"),houseId);

            }else{
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "本人暂未点播此歌或请输入完整歌名"),houseId);
            }
    }

    /**
     * 置顶播放列表的音乐
     *
     * @param music    music
     * @param accessor accessor
     */
    @MessageMapping("/music/top")
    public void topPickMusic(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if (!hasText(houseId)) {
            return;
        }
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试置顶音乐但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if (music == null || !hasText(music.getId())) {
                sessionService.send(sessionId, MessageType.NOTICE,
                        Response.failure((Object) null, "请输入完整歌曲信息"), houseId);
                return;
            }
            if (!musicService.topPickMusic(music,houseId)) {
                sessionService.send(sessionId, MessageType.NOTICE,
                        Response.failure((Object) null, "歌曲已不在待播列表，请刷新后重试"), houseId);
                return;
            }
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            log.info("session: {} 置顶音乐: {} 已成功, 即将广播置顶操作后的播放列表", sessionId, music.getName());
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "置顶成功"),houseId);
            sessionService.send(MessageType.PICK, Response.success(pickList, "置顶后的播放列表"),houseId);
        }
    }

    @MessageMapping("/music/black")
    public void black(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试拉黑音乐: {}, 没有权限, 已被阻止", sessionId, music.getId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            Long black = musicService.black(music.getId(),houseId);
            if (black > 0) {
                log.info("session: {} 尝试拉黑音乐: {} 已成功", sessionId, music.getName());
                sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "音乐拉黑成功"),houseId);
            } else {
                log.info("session: {} 尝试拉黑音乐: {} 已成功, 重复拉黑", sessionId, music.getName());
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "音乐重复拉黑"),houseId);
            }
        }
    }


    /**
     * 禁止切歌
     * @param accessor
     */
    @MessageMapping("/music/banswitch/{ban}")
    public void banswitch(@DestinationVariable boolean ban,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试禁止切歌, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setEnableSwitch(!ban,houseId);
            broadcastRoomState(houseId);
            if(ban){
                log.info("session: {} 禁止切歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "禁止切歌功能成功"),houseId);
            }else{
                log.info("session: {} 启用切歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "启用切歌功能成功"),houseId);
            }
           }
    }

    /**
     * 点赞模式的切换
     * @param accessor
     */
    @MessageMapping("/music/goodmodel/{good}")
    public void goodModel(@DestinationVariable boolean good,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试点赞模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setGoodModel(good,houseId);
            broadcastRoomState(houseId);
            if(good){
                log.info("session: {} 进入点赞模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入点赞模式"),houseId);
//                LinkedList<Music> pickList = musicService.getPickList(houseId);
                sessionService.send(MessageType.GOODMODEL, Response.success("GOOD", "goodlist"),houseId);

            }else{
                log.info("session: {} 退出点赞模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出点赞模式"),houseId);
                sessionService.send(MessageType.GOODMODEL, Response.success("EXITGOOD", "goodlist"),houseId);
            }
        }
    }

    /**
     * 点赞模式的切换
     * @param accessor
     */
    @MessageMapping("/music/musiccirclemodel/{circle}")
    public void musicCircleModel(@DestinationVariable boolean circle,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试单曲循环模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setMusicCircleModel(circle,houseId);
            configService.setListCircleModel(false,houseId);
            broadcastRoomState(houseId);
            if(circle){
                log.info("session: {} 进入单曲循环模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入单曲循环模式"),houseId);
            }else{
                log.info("session: {} 退出单曲循环模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出单曲循环模式"),houseId);
            }
        }
    }

    @MessageMapping("/music/musiclistmodel/{circle}")
    public void musicListModel(@DestinationVariable boolean circle,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试列表循环模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setListCircleModel(circle,houseId);
            configService.setMusicCircleModel(false,houseId);
            broadcastRoomState(houseId);
            if(circle){
                log.info("session: {} 进入列表循环模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入列表循环模式"),houseId);
            }else{
                log.info("session: {} 退出列表循环模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出列表循环模式"),houseId);
            }
        }
    }

    @MessageMapping("/music/randommodel/{random}")
    public void randomModel(@DestinationVariable boolean random,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试随机模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setRandomModel(random,houseId);
            broadcastRoomState(houseId);
            if(random){
                log.info("session: {} 进入随机模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入随机模式"),houseId);
            }else{
                log.info("session: {} 退出随机模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出随机模式"),houseId);
            }
        }
    }

    private void broadcastRoomState(String houseId) {
        sessionService.send(MessageType.ROOM_STATE,
                Response.success(com.scoder.jusic.model.RoomState.read(configService, houseId)), houseId);
    }

    @MessageMapping("/music/banchoose/{ban}")
    public void banchoose(@DestinationVariable boolean ban,StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试禁止点歌, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            configService.setEnableSearch(!ban,houseId);
            broadcastRoomState(houseId);
            if(ban){
                log.info("session: {} 禁止点歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "禁止点歌功能成功"),houseId);
            }else{
                log.info("session: {} 启用点歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "启用点歌功能成功"),houseId);
            }
        }
    }

    @MessageMapping("/music/pickwait/{minutes}")
    public void pickWait(@DestinationVariable int minutes, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试设置进房点歌等待时间, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "你没有权限"), houseId);
            return;
        }
        if (minutes < 0 || minutes > MAX_PICK_WAIT_MINUTES) {
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "点歌等待时间必须在0到1440分钟之间"), houseId);
            return;
        }
        configService.setPickWaitMinutes(minutes, houseId);
        sessionService.send(MessageType.NOTICE,
                Response.success((Object) null, minutes == 0
                        ? "已取消进房点歌等待限制"
                        : "已设置进入房间" + minutes + "分钟后可点歌"), houseId);
        broadcastRoomState(houseId);
    }

    @MessageMapping("/music/clearqueueonleave/{enabled}")
    public void clearQueueOnLeave(@DestinationVariable boolean enabled,
                                  StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试设置离房清理点歌, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "你没有权限"), houseId);
            return;
        }
        configService.setClearQueuedOnLeave(enabled, houseId);
        sessionService.send(MessageType.NOTICE,
                Response.success((Object) null, enabled
                        ? "已开启成员离房自动清理点歌"
                        : "已关闭成员离房自动清理点歌"), houseId);
        broadcastRoomState(houseId);
    }

    @MessageMapping("/music/defaultplaylistmode/{random}")
    public void defaultPlaylistMode(@DestinationVariable boolean random,
                                    StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试设置默认列表播放模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "你没有权限"), houseId);
            return;
        }
        configService.setDefaultPlaylistRandom(random, houseId);
        sessionService.send(MessageType.NOTICE,
                Response.success((Object) null, random
                        ? "默认列表已切换为随机不重复播放"
                        : "默认列表已切换为顺序播放"), houseId);
        broadcastRoomState(houseId);
    }

    @MessageMapping("/music/unblack")
    public void unblack(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试拉黑音乐但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            Long unblack = musicService.unblack(music.getId(),houseId);
            if (unblack > 0) {
                log.info("session: {} 尝试漂白音乐: {} 已成功", sessionId, music.getName());
                sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "音乐漂白成功"),houseId);
            } else {
                log.info("session: {} 尝试漂白音乐: {} 漂白异常, 可能不在黑名单中", sessionId, music.getName());
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "音乐漂白异常, 可能不在黑名单中"),houseId);
            }
        }
    }

    @MessageMapping("/music/setDefaultPlaylist")
    public void setDefaultPlaylist(SongList songList, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试设置默认播放列表但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if(songList.getId() == null || "".equals(songList.getId())){
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "歌单id不能为空"),houseId);
            }else{
                if(StringUtils.isPlayListIds(songList.getId())){
                    String[] idsStr = StringUtils.splitPlayListIds(songList.getId());
                    long count = musicService.addDefaultPlayList(houseId,idsStr,songList.getSource());
                    sessionService.send(MessageType.DEFAULT_PLAYLIST_CHANGED, Response.success(houseId), houseId);
                    sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "已添加"+count+"首歌至默认播放列表"),houseId);
                }else{
                    sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "多个歌单id以逗号或者空格隔开"),houseId);
                }
            }
        }
    }

    @MessageMapping("/music/blackmusic")
    public void blackmusic(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            log.info("session: {} 尝试展示黑名单但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            String blackMusic = musicService.showBlackMusic(houseId);
            if(blackMusic != null && !"".equals(blackMusic)){
                sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, blackMusic),houseId);
            }else{
                sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "暂无拉黑列表"),houseId);
            }
        }
    }

    @MessageMapping("/music/playlistSize")
    public void playlistSize(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!RoomRolePolicy.isPrivileged(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            Long size = musicService.playlistSize(houseId);
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "默认列表歌曲数"+size),houseId);
        }
    }

    @MessageMapping("/music/search")
    public void search(Music music, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        boolean allowBlank = music != null
                && ("lz".equals(music.getSource()) || "ai".equals(music.getSource()));
        String validationMessage = inboundPayloadValidator.validateSearch(
                music == null ? null : music.getName(), allowBlank, hulkPage);
        if (validationMessage == null) {
            validationMessage = inboundPayloadValidator.validateMetadata(
                    music == null ? null : music.getSource());
        }
        if (validationMessage != null) {
            log.info("session: {} 尝试搜索音乐, 但请求参数不合法", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, validationMessage),houseId);
            return;
        }
        try {
            Page<List<Music>> page = providerCallExecutor.search(
                    () -> musicService.search(music, hulkPage));
            log.info("session: {} 搜索音乐完成，即将向该用户推送结果", sessionId);
            sessionService.send(sessionId, MessageType.SEARCH, Response.success(page, "搜索结果"),houseId);
        } catch (RuntimeException ex) {
            log.warn("搜索音乐失败，source: {}, reason: {}", music.getSource(), ex.getMessage());
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "搜索失败,请稍后重试"),houseId);
        }
    }

    @MessageMapping("/music/searchsonglist")
    public void searchsonglist(SongList songList, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        boolean allowBlank = songList != null
                && !("qq_user".equals(songList.getSource()) || "wy_user".equals(songList.getSource()));
        String validationMessage = inboundPayloadValidator.validateSearch(
                songList == null ? null : songList.getName(), allowBlank, hulkPage);
        if (validationMessage == null) {
            validationMessage = inboundPayloadValidator.validateMetadata(
                    songList == null ? null : songList.getSource());
        }
        if (validationMessage != null) {
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, validationMessage),houseId);
            return;
        }
        try {
            Page<List<SongList>> page = providerCallExecutor.search(
                    () -> musicService.search(songList, hulkPage));
            log.info("session: {} 搜索歌单完成，即将向该用户推送结果", sessionId);
            sessionService.send(sessionId, MessageType.SEARCH_SONGLIST,
                    Response.success(page, "搜索结果"),houseId);
        } catch (RuntimeException ex) {
            log.warn("搜索歌单失败，source: {}, reason: {}", songList.getSource(), ex.getMessage());
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "搜索失败,请稍后重试"),houseId);
        }
    }

    @MessageMapping("/music/searchuser")
    public void searchuser(MusicUser musicUser, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String validationMessage = inboundPayloadValidator.validateSearch(
                musicUser == null ? null : musicUser.getNickname(), false, hulkPage);
        if (validationMessage == null) {
            validationMessage = inboundPayloadValidator.validateMetadata(
                    musicUser == null ? null : musicUser.getSource());
        }
        if (validationMessage != null) {
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, validationMessage),houseId);
            return;
        }
        try {
            Page<List<MusicUser>> page = providerCallExecutor.search(
                    () -> musicService.search(musicUser, hulkPage));
            log.info("session: {} 搜索用户完成，即将向该用户推送结果", sessionId);
            sessionService.send(sessionId, MessageType.SEARCH_USER,
                    Response.success(page, "搜索结果"),houseId);
        } catch (RuntimeException ex) {
            log.warn("搜索用户失败，source: {}, reason: {}", musicUser.getSource(), ex.getMessage());
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "搜索失败,请稍后重试"),houseId);
        }
    }

}

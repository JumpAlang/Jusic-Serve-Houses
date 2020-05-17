package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.service.ChatService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author H
 */
@Controller
@Slf4j
public class ChatController {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private ChatService chatService;
    private static final List<String> roles = new ArrayList<String>() {{
        add("root");
        add("admin");
    }};

    @MessageMapping("/chat")
    public void chat(Chat chat, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        User user = sessionService.getUser(sessionId);
        User black = sessionService.getBlack(sessionId);
        long currentTime = System.currentTimeMillis();
        if (null != black && black.getSessionId().equals(sessionId)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你已被拉黑"));
        } else if (null != user.getLastMessageTime() && currentTime - user.getLastMessageTime() < 2000) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "发言时间间隔太短"));
        } else {
            chat.setSessionId(sessionId);
            chat.setNickName(user.getNickName());
            sessionService.send(MessageType.CHAT, Response.success(chat));
            sessionService.setLastMessageTime(user, currentTime);
        }
    }

    @MessageMapping("/chat/black")
    public void black(User user, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String role = sessionService.getRole(sessionId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试拉黑用户: {}, 已被阻止", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"));
        } else {
            User black = sessionService.getUser(user.getSessionId());
            if(black == null){
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "用户已经退出来了"));
            }else{
                sessionService.black(black);
                log.info("session: {} 拉黑用户: {}, 已成功", sessionId, user.getSessionId());
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "拉黑成功"));
            }
         }
    }

    @MessageMapping("/chat/unblack")
    public void unblack(User user, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String role = sessionService.getRole(sessionId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试解除黑名单: {}, 已被阻止", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"));
        } else {
            sessionService.unblack(user.getSessionId());
            log.info("session: {} 用户: {} 已被移除黑名单", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "已移除黑名单"));
        }
    }

    @MessageMapping("/chat/blackuser")
    public void blackmusic(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String role = sessionService.getRole(sessionId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"));
        } else {
            String blackUser = sessionService.showBlackUser();
            if(blackUser!=null && !"".equals(blackUser)){
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, blackUser));
            }else{
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "暂无拉黑列表"));
            }
        }
    }

    @MessageMapping("/chat/picture/search")
    public void pictureSearch(Chat chat, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        if (Objects.isNull(chat) || Objects.isNull(chat.getContent())) {
            log.info("session: {} 尝试搜索图片, 但关键字为空", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "请输入要搜索的关键字"));
        } else {
            Page<List> page = chatService.pictureSearch(chat.getContent(), hulkPage);
            log.info("session: {} 尝试搜索图片, 关键字: {}, 即将向该用户推送结果", sessionId, chat.getContent());
            sessionService.send(sessionId, MessageType.SEARCH_PICTURE, Response.success(page, "搜索结果"));
        }
    }

}

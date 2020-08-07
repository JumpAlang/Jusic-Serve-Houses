package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.*;
import com.scoder.jusic.service.ChatService;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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
    private HouseContainer houseContainer;
    @Autowired
    private ChatService chatService;
    private static final List<String> roles = new ArrayList<String>() {{
        add("root");
        add("admin");
    }};

    @MessageMapping("/chat/notice/{msg}")
    @SendTo("/topic/chat")
    public Response notice(@DestinationVariable String msg, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!roles.get(0).equals(role)) {
            return Response.failure((Object) null, "你没有权限");
        }

        return Response.success(msg, "通知成功");
    }

    @MessageMapping("/chat")
    public void chat(Chat chat, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        User user = sessionService.getUser(sessionId,houseId);
//        User black = sessionService.getBlack(sessionId,houseId);
        long currentTime = System.currentTimeMillis();
//        if (null != black && black.getSessionId().equals(sessionId)) {
//            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你已被拉黑"),houseId);
//        } else
        if (null != user.getLastMessageTime() && currentTime - user.getLastMessageTime() < 2000) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "发言时间间隔太短"),houseId);
        } else {
            chat.setSessionId(sessionId);
            chat.setNickName(user.getNickName());
            String atsession = StringUtils.getSessionId(chat.getContent());
            if(atsession != null && atsession != ""){
                User atUser = sessionService.getUser(atsession,houseId);
                if(atUser != null){
                    sessionService.send(atsession,MessageType.CHAT, Response.success(chat),houseId);
                    sessionService.send(sessionId,MessageType.CHAT, Response.success(chat),houseId);
                }else{
                    sessionService.send(MessageType.CHAT, Response.success(chat),houseId);
                }
            }else{
                sessionService.send(MessageType.CHAT, Response.success(chat),houseId);
            }
            sessionService.setLastMessageTime(user, currentTime,houseId);
        }
    }

    @MessageMapping("/chat/black")
    public void black(User user, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试拉黑用户: {}, 已被阻止", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            User black = sessionService.getUser(user.getSessionId(),houseId);
            if(black == null){
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "用户已经退出来了"),houseId);
            }else{

                sessionService.black(black,houseId);
                log.info("session: {} 拉黑用户: {}, 已成功", sessionId, user.getSessionId());
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "拉黑成功"),houseId);
            }
         }
    }

    @MessageMapping("/chat/announce")
    public void announcement(Message message, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {

            message.setSendTime(System.currentTimeMillis());
            User user = sessionService.getUser(sessionId,houseId);
            message.setNickName(user.getNickName());
            message.setSessionId(user.getSessionId());
            sessionService.send(MessageType.ANNOUNCEMENT, Response.success(message, "发布成功"),houseId);
            message.setPushTime(System.currentTimeMillis());
            House house = houseContainer.get(houseId);
            house.setAnnounce(message);
        }
    }


    @MessageMapping("/chat/unblack")
    public void unblack(User user, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试解除黑名单: {}, 已被阻止", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            sessionService.unblack(user.getSessionId(),houseId);
            log.info("session: {} 用户: {} 已被移除黑名单", sessionId, user.getSessionId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "已移除黑名单"),houseId);
        }
    }

    @MessageMapping("/chat/blackuser")
    public void blackmusic(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            String blackUser = sessionService.showBlackUser(houseId);
            if(blackUser!=null && !"".equals(blackUser)){
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, blackUser),houseId);
            }else{
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "暂无拉黑列表"),houseId);
            }
        }
    }

    @MessageMapping("/chat/picture/search")
    public void pictureSearch(Chat chat, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if (Objects.isNull(chat) || Objects.isNull(chat.getContent())) {
            log.info("session: {} 尝试搜索图片, 但关键字为空", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "请输入要搜索的关键字"),houseId);
        } else {
            Page<List> page = chatService.pictureSearch(chat.getContent(), hulkPage);
            log.info("session: {} 尝试搜索图片, 关键字: {}, 即将向该用户推送结果", sessionId, chat.getContent());
            sessionService.send(sessionId, MessageType.SEARCH_PICTURE, Response.success(page, "搜索结果"),houseId);
        }
    }

}

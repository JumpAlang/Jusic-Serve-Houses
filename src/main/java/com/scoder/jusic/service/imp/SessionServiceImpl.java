package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSON;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

/**
 * @author H
 */
@Service
@Slf4j
public class SessionServiceImpl implements SessionService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionBlackRepository sessionBlackRepository;

    @Override
    public void settingName(String sessionId, String name) {
        if (null == name) {

        } else {
            log.info("setting name: {}", name);
            User user = sessionRepository.getSession(sessionId);
            user.setName(name);
            user.setNickName(user.getName() + "(" + StringUtils.desensitizeIPV4(user.getRemoteAddress()) + ")");
            sessionRepository.setSession(user);
        }
    }

    @Override
    public String getRole(String sessionId) {
        User session = sessionRepository.getSession(sessionId);
        if(session != null){
            return session.getRole();
        }else{
            return "";
        }
    }

    @Override
    public void putSession(WebSocketSession session) {
        jusicProperties.getSessions().put(session.getId(), session);
        User user = User.builder()
                .sessionId(session.getId())
                .name("")
                .nickName("")
                .remoteAddress(session.getAttributes().get("remoteAddress").toString())
                .role("default")
                .build();
        sessionRepository.setSession(user);
    }

    @Override
    public void clearSession(WebSocketSession session) {
        log.info("Clear Session: {}", session.getId());
        jusicProperties.getSessions().remove(session.getId());
        sessionRepository.removeSession(session.getId());
    }

    @Override
    public void send(Object payload) {
        this.send(MessageType.NOTICE, payload);
    }

    @Override
    public void send(MessageType messageType, Object payload) {
        Map<String, WebSocketSession> sessions = jusicProperties.getSessions();
        sessions.forEach((key, session) -> {
            if (session.isOpen()) {
                this.send(session, messageType, payload);
            }
        });
    }

    @Override
    public void send(String sessionId, Object payload) {
        this.send(sessionId, MessageType.NOTICE, payload);
    }

    @Override
    public void send(String sessionId, MessageType messageType, Object payload) {
        Map<String, WebSocketSession> sessions = jusicProperties.getSessions();
        WebSocketSession session = sessions.get(sessionId);
        if(session != null){
            this.send(session, messageType, payload);
        }
    }

    @Override
    public void send(WebSocketSession session, Object payload) {
        this.send(session, MessageType.NOTICE, payload);
    }

    @Override
    public void send(WebSocketSession session, MessageType messageType, Object payload) {
        TextMessage textMessage = new TextMessage(this.getPayload(messageType.type(), payload));
        try {
            synchronized (session) {
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            log.error("send exception.");
        }
    }

    @Override
    public String getNickName(String sessionId) {
        User session = sessionRepository.getSession(sessionId);
        if(session != null){
            return session.getNickName();
        }else{
            return "";
        }
    }

    @Override
    public void setLastMessageTime(User user, Long time) {
        user.setLastMessageTime(time);
        sessionRepository.setSession(user);
    }

    @Override
    public User getUser(String sessionId) {
        return sessionRepository.getSession(sessionId);
    }

    @Override
    public void black(User user) {
        sessionBlackRepository.setSession(user);
    }

    @Override
    public String showBlackUser() {
        Set blackList = sessionBlackRepository.showBlackList();
        if(blackList != null && blackList.size() > 0) {
            return String.join(",", blackList);
        }
        return null;
    }



    @Override
    public User getBlack(String sessionId) {
        return sessionBlackRepository.getSession(sessionId);
    }

    @Override
    public void unblack(String sessionId) {
        sessionBlackRepository.removeSession(sessionId);
    }

    @Override
    public Long size() {
        return sessionRepository.size();
    }

    /**
     * 自定义消息格式，模仿 stomp 的格式
     * </p>
     * 详见：http://stomp.github.io/stomp-specification-1.2.html#MESSAGE
     *
     * @param content payload 要发送的消息内容
     * @return 格式化后的字符串，可以直接作为消息发送出去
     */
    private String getPayload(String messageType, Object content) {
        StringBuilder payload = new StringBuilder();
        String jsonString = JSON.toJSONString(content);
        payload.append(messageType)
                .append("\n")
                .append("content-type:application/json");
        try {
            payload.append("\n")
                    .append("content-length:").append(StringUtils.getLength(jsonString, StringUtils.ENCODE_UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        payload.append("\n")
                .append("\n")
                .append(jsonString);
        return payload.toString();
    }

}

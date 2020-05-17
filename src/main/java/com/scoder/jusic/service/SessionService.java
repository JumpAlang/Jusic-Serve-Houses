package com.scoder.jusic.service;

import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author H
 */
public interface SessionService {

    /**
     * update name
     *
     * @param sessionId session id
     * @param name      name
     */
    void settingName(String sessionId, String name);

    /**
     * get role by session
     *
     * @param sessionId session id
     * @return role: root | admin |default
     */
    String getRole(String sessionId);

    /**
     * put session.
     *
     * @param session the client session
     */
    void putSession(WebSocketSession session);

    /**
     * clear session.
     *
     * @param session the client session
     */
    void clearSession(WebSocketSession session);

    /**
     * send message.
     *
     * @param payload payload
     */
    void send(Object payload);

    /**
     * send message.
     *
     * @param messageType first
     * @param payload     payload
     */
    void send(MessageType messageType, Object payload);

    /**
     * send message.
     *
     * @param sessionId session id
     * @param payload   payload
     */
    void send(String sessionId, Object payload);

    /**
     * send message.
     *
     * @param session the client session
     * @param payload payload
     */
    void send(WebSocketSession session, Object payload);

    /**
     * send message.
     *
     * @param sessionId   session id
     * @param messageType message type
     * @param payload     payload
     */
    void send(String sessionId, MessageType messageType, Object payload);

    /**
     * send message.
     *
     * @param session     session
     * @param messageType message type
     * @param payload     payload
     */
    void send(WebSocketSession session, MessageType messageType, Object payload);

    /**
     * get nick name
     *
     * @param sessionId the client session
     * @return nick name
     */
    String getNickName(String sessionId);

    /**
     * 最后发言时间
     *
     * @param user user
     * @param time time
     */
    void setLastMessageTime(User user, Long time);

    /**
     * get user
     *
     * @param sessionId session id
     * @return -
     */
    User getUser(String sessionId);

    /**
     * black
     *
     * @param user session id
     */
    void black(User user);

    String showBlackUser();


    /**
     * get black user
     *
     * @param sessionId the client session id
     * @return black user
     */
    User getBlack(String sessionId);

    /**
     * unblack
     *
     * @param sessionId the client session id
     */
    void unblack(String sessionId);

    /**
     * size
     *
     * @return long
     */
    Long size();

}

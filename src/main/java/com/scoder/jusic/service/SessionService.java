package com.scoder.jusic.service;

import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

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
    void settingName(String sessionId, String name,String houseId);

    /**
     * get role by session
     *
     * @param sessionId session id
     * @return role: root | admin |default
     */
    String getRole(String sessionId,String houseId);

    /**
     * put session.
     *
     * @param session the client session
     */
    User putSession(WebSocketSession session,String houseId);

    /**
     * clear session.
     *
     * @param session the client session
     */
    void clearSession(WebSocketSession session,String houseId);

    WebSocketSession clearSession(String sessionId,String houseId);

    /**
     * send message.
     *
     * @param payload payload
     */
    void send(Object payload,String houseId);

    /**
     * send message.
     *
     * @param messageType first
     * @param payload     payload
     */
    void send(MessageType messageType, Object payload,String houseId);

    void send(MessageType messageType, Object payload);


    /**
     * send message.
     *
     * @param sessionId session id
     * @param payload   payload
     */
    void send(String sessionId, Object payload,String houseId);

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
    void send(String sessionId, MessageType messageType, Object payload,String houseId);

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
    String getNickName(String sessionId,String houseId);

    /**
     * 最后发言时间
     *
     * @param user user
     * @param time time
     */
    void setLastMessageTime(User user, Long time,String houseId);

    /**
     * get user
     *
     * @param sessionId session id
     * @return -
     */
    User getUser(String sessionId,String houseId);

    /**
     * black
     *
     * @param user session id
     */
    void black(User user,String houseId);

    String showBlackUser(String houseId);


    /**
     * get black user
     *
     * @param sessionId the client session id
     * @return black user
     */
    User getBlack(String sessionId,String ip,String houseId);

    /**
     * unblack
     *
     * @param sessionId the client session id
     */
    void unblack(String sessionId,String houseId);

    /**
     * size
     *
     * @return long
     */
    Long size(String houseId);

    List getSession(String houseId);
}

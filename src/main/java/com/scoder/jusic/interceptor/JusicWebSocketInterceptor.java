package com.scoder.jusic.interceptor;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * @author H
 */
@Component
public class JusicWebSocketInterceptor implements ChannelInterceptor {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private HouseContainer houseContainer;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String ip = (String)(accessor.getSessionAttributes().get("remoteAddress"));
        User black = sessionService.getBlack(sessionId,ip,houseId);
        if(houseId == null || houseContainer.get(houseId) == null){
            return null;
        }
        if (null != black) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你已被管理员拉黑"),houseId);
            return null;
        }
//        System.out.println("presend"+message);
        return message;
    }

//    @Override
//    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
//        System.out.println("postsend"+message);
//    }
//
//    @Override
//    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
//        System.out.println("afterSendCompletion"+message);
//    }
//
//    @Override
//    public boolean preReceive(MessageChannel channel) {
//        System.out.println("preReceive"+channel);
//        return false;
//    }
//
//    @Override
//    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
//        System.out.println("postReceive"+message);
//        return null;
//    }
//
//    @Override
//    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
//        System.out.println("afterReceiveCompletion"+message);
//    }
}

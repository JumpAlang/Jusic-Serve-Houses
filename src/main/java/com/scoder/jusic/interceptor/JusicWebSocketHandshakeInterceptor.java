package com.scoder.jusic.interceptor;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.ClientIpResolver;
import com.scoder.jusic.security.RateLimitDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 自定义 WebSocket 握手拦截器
 *
 * @author H
 */
@Component
@Slf4j
public class JusicWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private AbuseProtectionService abuseProtectionService;
    @Autowired
    private ClientIpResolver clientIpResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        String remoteAddress = clientIpResolver.resolve(request);
        attributes.put("remoteAddress", remoteAddress);
        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();
        Object clientId = httpRequest.getAttribute(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE);
        if (clientId != null && !clientId.toString().trim().isEmpty()) {
            attributes.put(ClientIdentityFilter.CLIENT_ID_ATTRIBUTE,
                    clientId.toString());
        }
        String houseId = httpRequest.getParameter("houseId");
        if(houseId == null || "".equals(houseId)){
            houseId = JusicProperties.HOUSE_DEFAULT_ID;
        }

        RateLimitDecision handshakeDecision = abuseProtectionService.check(
                AbuseAction.HANDSHAKE, null, remoteAddress, null);
        if (!handshakeDecision.isAllowed()) {
            log.warn("websocket handshake rejected by rate limit");
            return false;
        }

        if (JusicProperties.HOUSE_DEFAULT_ID.equals(houseId)) {
            if (!allowKnownRoomHandshake(houseId)) {
                return false;
            }
            attributes.put("houseId",houseId);
            return true;
        }else{
            House house = houseContainer.get(houseId);
            if(house == null){
                return false;
            }else{
               if (!allowKnownRoomHandshake(houseId)) {
                   return false;
               }
               if(Boolean.TRUE.equals(house.getNeedPwd())){
                    RateLimitDecision enterDecision = abuseProtectionService.check(
                            AbuseAction.ROOM_ENTER, null, remoteAddress, houseId);
                    if (!enterDecision.isAllowed()) {
                        log.warn("websocket room password check rejected by rate limit");
                        return false;
                    }
                    String housePwd = httpRequest.getParameter("housePwd");
                    if(house.getPassword() == null || !house.getPassword().equals(housePwd)){
                        return false;
                    }
                }
                String connectType = httpRequest.getParameter("connectType");
                attributes.put("connectType",connectType);
                attributes.put("houseId",houseId);
                return true;
            }
        }
    }

    /**
     * The first handshake check is IP-only so random room ids cannot create
     * unbounded Redis keys. Once the room is known, this second check passes no
     * IP and therefore consumes only the shared room handshake bucket.
     */
    private boolean allowKnownRoomHandshake(String houseId) {
        RateLimitDecision roomDecision = abuseProtectionService.check(
                AbuseAction.HANDSHAKE, null, null, houseId);
        if (!roomDecision.isAllowed()) {
            log.warn("websocket handshake rejected by room rate limit");
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

}

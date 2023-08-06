package com.scoder.jusic.interceptor;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.util.IPUtils;
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

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        attributes.put("remoteAddress", IPUtils.getRemoteAddress(request));
        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String houseId = httpRequest.getParameter("houseId");
        if(houseId == null || "".equals(houseId)){
            houseId = JusicProperties.HOUSE_DEFAULT_ID;
            attributes.put("houseId",houseId);
            return true;
        }else{
            House house = houseContainer.get(houseId);
            if(house == null){
                return false;
            }else{
               if(house.getNeedPwd()){
                    String housePwd = httpRequest.getParameter("housePwd");
                    if(!house.getPassword().equals(housePwd)){
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

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

}

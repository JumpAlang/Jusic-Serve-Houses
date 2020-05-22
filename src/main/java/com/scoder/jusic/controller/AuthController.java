package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.Auth;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.service.AuthService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * @author H
 */
@Controller
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private SessionService sessionService;

    private static final List<String> roles = new ArrayList<String>() {{
        add("root");
        add("admin");
    }};

    @MessageMapping("/auth/root")
    public void authRoot(Auth auth, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        boolean result = authService.authRoot(sessionId, auth.getPassword(),houseId);
        sessionService.send(sessionId,
                MessageType.AUTH_ROOT,
                result ? Response.success((Object) null, "登录成功") : Response.failure((Object) null, "登录失败"),houseId);
    }

    @MessageMapping("/auth/admin")
    public void authAdmin(Auth auth, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        boolean result = authService.authAdmin(sessionId, auth.getPassword(), houseId);
        sessionService.send(sessionId,
                MessageType.AUTH_ADMIN,
                result ? Response.success((Object) null, "登录成功") : Response.failure((Object) null, "登录失败"),houseId);
    }

    @MessageMapping("/auth/adminpwd/{password}")
    public void adminpwd(@DestinationVariable String password, StompHeaderAccessor accessor) {

        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (password == null || password == "") {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "密码不能为空"),houseId);
            return;
        }
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            authService.setAdminPassword(password,houseId);
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "密码修改成功"),houseId);
        }
    }

    @MessageMapping("/auth/rootpwd/{password}")
    public void rootpwd(@DestinationVariable String password, StompHeaderAccessor accessor) {

        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (password == null || password == "") {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "密码不能为空"),houseId);
            return;
        }
        if (!roles.get(0).equals(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        }else{
            authService.setRootPassword(password,houseId);
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "密码修改成功"),houseId);
        }
    }

}

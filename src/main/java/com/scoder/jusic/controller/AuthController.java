package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.Auth;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.service.AuthService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

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

    @MessageMapping("/auth/root")
    public void authRoot(Auth auth, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        boolean result = authService.authRoot(sessionId, auth.getPassword());
        sessionService.send(sessionId,
                MessageType.AUTH_ROOT,
                result ? Response.success((Object) null, "登录成功") : Response.failure((Object) null, "登录失败"));
    }

    @MessageMapping("/auth/admin")
    public void authAdmin(Auth auth, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        boolean result = authService.authAdmin(sessionId, auth.getPassword());
        sessionService.send(sessionId,
                MessageType.AUTH_ADMIN,
                result ? Response.success((Object) null, "登录成功") : Response.failure((Object) null, "登录失败"));
    }

}

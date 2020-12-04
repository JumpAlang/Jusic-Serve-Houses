package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.Auth;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.service.AuthService;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.util.StringUtils;
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
    private HouseContainer houseContainer;
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
            House house = houseContainer.get(houseId);
            if(house.getForbiddenModiPwd() != null && house.getForbiddenModiPwd() && !roles.get(0).equals(role)){
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "房间不支持修改密码"),houseId);
                return;
            }
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

    @MessageMapping("/auth/setPicker/{userId}")
    public void setPicker(@DestinationVariable String userId, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if(StringUtils.isSessionId(userId)){
                User user = sessionService.getUser(userId,houseId);
                if(user != null){
                    if(user.getRole().indexOf("picker") == -1){
                        user.setRole(user.getRole()+",picker");
                        authService.updateUser(user,houseId);
                    }
                    sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "设置点歌人成功"),houseId);
                }else{
                    sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "用户不存在"),houseId);
                }
            }else{
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "请填写正确的用户id"),houseId);
            }

        }
    }

    @MessageMapping("/auth/setNoPicker/{userId}")
    public void setNoPicker(@DestinationVariable String userId, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if(StringUtils.isSessionId(userId)){
                User user = sessionService.getUser(userId,houseId);
                if(user != null){
                    if(user.getRole().indexOf("picker") != -1){
                        user.setRole(user.getRole().replaceAll("picker",""));
                        authService.updateUser(user,houseId);
                    }
                    sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "取消点歌人成功"),houseId);
                }else{
                    sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "用户不存在"),houseId);
                }
            }else{
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "请填写正确的用户id"),houseId);
            }

        }
    }

    @MessageMapping("/auth/setVoter/{userId}")
    public void setVoter(@DestinationVariable String userId, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if(StringUtils.isSessionId(userId)){
                User user = sessionService.getUser(userId,houseId);
                if(user != null){
                    if(user.getRole().indexOf("voter") == -1){
                        user.setRole(user.getRole()+",voter");
                        authService.updateUser(user,houseId);
                    }
                    sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "设置切歌人成功"),houseId);
                }else{
                    sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "用户不存在"),houseId);
                }
            }else{
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "请填写正确的用户id"),houseId);
            }

        }
    }

    @MessageMapping("/auth/setNoVoter/{userId}")
    public void setNoVoter(@DestinationVariable String userId, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            if(StringUtils.isSessionId(userId)){
                User user = sessionService.getUser(userId,houseId);
                if(user != null){
                    if(user.getRole().indexOf("voter") != -1){
                        user.setRole(user.getRole().replaceAll("voter",""));
                        authService.updateUser(user,houseId);
                    }
                    sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "取消切歌人成功"),houseId);
                }else{
                    sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "用户不存在"),houseId);
                }
            }else{
                sessionService.send(sessionId,MessageType.NOTICE, Response.failure((Object) null, "请填写正确的用户id"),houseId);
            }

        }
    }

}

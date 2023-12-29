package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Setting;
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
public class ConfigController {

    @Autowired
    private SessionService sessionService;

    @MessageMapping("/setting/name")
    public void settingName(Setting setting, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String name = setting.getName();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        if (name == null || "".equals(name) || name.length() > 33) {
            sessionService.send(sessionId, MessageType.SETTING_NAME, Response.failure((Object) null, "昵称设置失败"),houseId);
        } else {
            log.info("设置用户名: {}", name);
            sessionService.settingName(sessionId, name,houseId);
            sessionService.send(sessionId, MessageType.SETTING_NAME, Response.success(setting, "昵称设置成功"),houseId);
        }
    }

}

package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.scoder.jusic.service.SessionService;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DefaultPlaylistController {
    private final MusicDefaultRepository repository;
    private final SessionService sessions;
    public DefaultPlaylistController(MusicDefaultRepository repository, SessionService sessions) {
        this.repository = repository;
        this.sessions = sessions;
    }

    @Data
    public static class Request {
        private Integer pageIndex = 1;
        private Integer pageSize = 30;
        private String requestId;
    }

    @MessageMapping("/music/defaultPlaylist")
    public void list(Request request, StompHeaderAccessor accessor) {
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("houseId", houseId);
        if (request != null && request.getRequestId() != null && request.getRequestId().length() <= 64)
            result.put("requestId", request.getRequestId());
        if (request == null || request.getPageIndex() == null || request.getPageSize() == null
                || request.getPageIndex() < 1 || request.getPageIndex() > 100000
                || request.getPageSize() < 1 || request.getPageSize() > 50) {
            sessions.send(accessor.getSessionId(), MessageType.DEFAULT_PLAYLIST,
                    Response.failure(result, "分页参数无效"), houseId);
            return;
        }
        try {
            Long roomSize = repository.size(houseId);
            result.put("scope", "room");
            result.put("total", roomSize == null ? 0L : roomSize);
            result.put("pageIndex", request.getPageIndex());
            result.put("pageSize", request.getPageSize());
            result.put("songs", repository.page(houseId, (request.getPageIndex()-1L)*request.getPageSize(), request.getPageSize()));
            sessions.send(accessor.getSessionId(), MessageType.DEFAULT_PLAYLIST, Response.success(result), houseId);
        } catch (RuntimeException ex) {
            sessions.send(accessor.getSessionId(), MessageType.DEFAULT_PLAYLIST,
                    Response.failure(result, "默认列表读取失败，请重试"), houseId);
        }
    }
}

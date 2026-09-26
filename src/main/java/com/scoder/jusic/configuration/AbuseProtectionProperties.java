package com.scoder.jusic.configuration;

import com.scoder.jusic.security.AbuseAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Abuse protection defaults are intentionally conservative and can be
 * overridden with standard Spring Boot configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "jusic.abuse")
@Data
public class AbuseProtectionProperties {

    private boolean enabled = true;
    private String namespace = "jusic:guard:v1";
    private InputLimits input = new InputLimits();

    private ActionLimit handshake = ActionLimit.of(null, scope(30, 60_000), scope(3_000, 60_000), true);
    private ActionLimit message = ActionLimit.of(scope(120, 60_000), scope(600, 60_000), scope(10_000, 60_000), true);
    private ActionLimit chat = ActionLimit.of(scope(1, 2_000), scope(60, 60_000), scope(100, 1_000), true);
    private ActionLimit pickMusic = ActionLimit.of(scope(10, 60_000), scope(30, 600_000), scope(60, 60_000), false);
    private ActionLimit search = ActionLimit.of(scope(30, 60_000), scope(100, 60_000), scope(60, 60_000), false);
    private ActionLimit vote = ActionLimit.of(scope(30, 60_000), scope(120, 60_000), scope(100, 1_000), true);
    private ActionLimit auth = ActionLimit.of(scope(5, 600_000), scope(20, 600_000), scope(50, 600_000), false);
    private ActionLimit adminNotify = ActionLimit.of(scope(1, 300_000), scope(3, 3_600_000), scope(10, 3_600_000), false);
    private ActionLimit roomCreate = ActionLimit.of(scope(2, 3_600_000), scope(2, 3_600_000), scope(100, 3_600_000), false);
    private ActionLimit roomEnter = ActionLimit.of(scope(30, 60_000), scope(60, 60_000), scope(50, 60_000), false);
    private ActionLimit houseQuery = ActionLimit.of(scope(60, 60_000), scope(120, 60_000), scope(600, 60_000), true);
    private ActionLimit miniCode = ActionLimit.of(scope(2, 60_000), scope(20, 86_400_000), scope(100, 86_400_000), false);
    private ActionLimit biliProxy = ActionLimit.of(scope(30, 60_000), scope(30, 60_000), scope(300, 60_000), false);
    private ActionLimit adminApi = ActionLimit.of(scope(30, 60_000), scope(30, 60_000), scope(300, 60_000), false);

    public ActionLimit policy(AbuseAction action) {
        if (action == null) {
            return message;
        }
        switch (action) {
            case HANDSHAKE:
                return handshake;
            case CHAT:
                return chat;
            case PICK_MUSIC:
                return pickMusic;
            case SEARCH:
                return search;
            case VOTE:
                return vote;
            case AUTH:
                return auth;
            case ADMIN_NOTIFY:
                return adminNotify;
            case ROOM_CREATE:
                return roomCreate;
            case ROOM_ENTER:
                return roomEnter;
            case HOUSE_QUERY:
                return houseQuery;
            case MINI_CODE:
                return miniCode;
            case BILI_PROXY:
                return biliProxy;
            case ADMIN_API:
                return adminApi;
            case MESSAGE:
            default:
                return message;
        }
    }

    private static ScopeLimit scope(int maxRequests, long windowMs) {
        return new ScopeLimit(maxRequests, windowMs);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeLimit {
        private int maxRequests;
        private long windowMs;

        public boolean isEnabled() {
            return maxRequests > 0 && windowMs > 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionLimit {
        private ScopeLimit session;
        private ScopeLimit ip;
        private ScopeLimit room;
        private boolean failOpen;

        private static ActionLimit of(ScopeLimit session, ScopeLimit ip, ScopeLimit room, boolean failOpen) {
            return new ActionLimit(session, ip, room, failOpen);
        }
    }

    /**
     * Limits are checked without rewriting an otherwise valid legacy payload.
     * Both code-point and UTF-8 byte limits are used so multi-byte input cannot
     * bypass the bound or create unexpectedly large WebSocket frames.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputLimits {
        private int chatCodePoints = 300;
        private int chatBytes = 1_024;
        private int searchCodePoints = 100;
        private int searchBytes = 512;
        private int musicNameCodePoints = 200;
        private int musicIdCodePoints = 160;
        private int metadataBytes = 512;
        private int maxPageIndex = 1_000;
        private int maxPageSize = 50;
    }
}

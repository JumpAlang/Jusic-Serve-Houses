package com.scoder.jusic.job;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.SessionReaperProperties;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Removes closed sessions that were retained locally after a close callback
 * was missed or interrupted.
 */
@Component
@Slf4j
public class SessionReaper {

    private final JusicProperties jusicProperties;
    private final SessionService sessionService;
    private final SessionReaperProperties properties;

    public SessionReaper(JusicProperties jusicProperties,
                         SessionService sessionService,
                         SessionReaperProperties properties) {
        this.jusicProperties = jusicProperties;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jusic.websocket.reaper.fixed-delay:15000}")
    public void reapClosedSessions() {
        if (!properties.isEnabled()) {
            return;
        }

        int scanned = 0;
        int reaped = 0;
        int failures = 0;
        RuntimeException firstFailure = null;

        Map<String, ConcurrentHashMap<String, WebSocketSession>> houses =
                jusicProperties.getSession().get();
        for (Map.Entry<String, ConcurrentHashMap<String, WebSocketSession>> houseEntry
                : houses.entrySet()) {
            String houseId = houseEntry.getKey();
            ConcurrentHashMap<String, WebSocketSession> sessions = houseEntry.getValue();
            if (sessions == null) {
                continue;
            }

            for (WebSocketSession session : sessions.values()) {
                scanned++;
                try {
                    if (!session.isOpen()) {
                        sessionService.clearSession(session, houseId);
                        reaped++;
                    }
                } catch (RuntimeException ex) {
                    failures++;
                    if (firstFailure == null) {
                        firstFailure = ex;
                    }
                }
            }
        }

        if (failures > 0) {
            log.warn("WebSocket session reaper completed with cleanup failures: "
                            + "scanned={}, reaped={}, failures={}",
                    scanned, reaped, failures);
            if (log.isDebugEnabled()) {
                log.debug("First WebSocket session reaper cleanup failure", firstFailure);
            }
        } else if (reaped > 0 && log.isDebugEnabled()) {
            log.debug("WebSocket session reaper removed closed sessions: scanned={}, reaped={}",
                    scanned, reaped);
        }
    }
}

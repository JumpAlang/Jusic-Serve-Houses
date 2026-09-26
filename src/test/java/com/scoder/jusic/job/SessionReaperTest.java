package com.scoder.jusic.job;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.SessionReaperProperties;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionReaperTest {

    @Test
    void propertiesHaveSafeEnabledDefaults() {
        SessionReaperProperties properties = new SessionReaperProperties();

        assertTrue(properties.isEnabled());
        assertEquals(15_000L, properties.getFixedDelay());
    }

    @Test
    void clearsClosedSessionsAndLeavesOpenSessionsAlone() {
        JusicProperties jusicProperties = new JusicProperties();
        SessionService sessionService = mock(SessionService.class);
        WebSocketSession closed = session(false);
        WebSocketSession open = session(true);
        jusicProperties.getSessions("house-a").put("closed", closed);
        jusicProperties.getSessions("house-a").put("open", open);

        reaper(jusicProperties, sessionService, true).reapClosedSessions();

        verify(sessionService).clearSession(closed, "house-a");
        verify(sessionService, never()).clearSession(open, "house-a");
    }

    @Test
    void oneCleanupFailureDoesNotStopTheRestOfTheScan() {
        JusicProperties jusicProperties = new JusicProperties();
        SessionService sessionService = mock(SessionService.class);
        jusicProperties.getSessions("house-a").put("closed-a", session(false));
        jusicProperties.getSessions("house-a").put("closed-b", session(false));
        doThrow(new IllegalStateException("cleanup failed"))
                .doNothing()
                .when(sessionService).clearSession(any(WebSocketSession.class), anyString());

        reaper(jusicProperties, sessionService, true).reapClosedSessions();

        verify(sessionService, times(2))
                .clearSession(any(WebSocketSession.class), anyString());
    }

    @Test
    void disabledReaperDoesNotInspectOrClearSessions() {
        JusicProperties jusicProperties = new JusicProperties();
        SessionService sessionService = mock(SessionService.class);
        WebSocketSession closed = session(false);
        jusicProperties.getSessions("house-a").put("closed", closed);

        reaper(jusicProperties, sessionService, false).reapClosedSessions();

        verifyNoInteractions(sessionService);
        verify(closed, never()).isOpen();
    }

    private static SessionReaper reaper(JusicProperties jusicProperties,
                                         SessionService sessionService,
                                         boolean enabled) {
        SessionReaperProperties properties = new SessionReaperProperties();
        properties.setEnabled(enabled);
        return new SessionReaper(jusicProperties, sessionService, properties);
    }

    private static WebSocketSession session(boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(open);
        return session;
    }
}

package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.ClientConnectionRegistry;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceImplNullSafetyTest {

    @Test
    void enteringRoomStoresJoinTimeOnUserMembership() {
        SessionRepository sessionRepository = mock(SessionRepository.class);
        JusicProperties properties = new JusicProperties();
        SessionServiceImpl service = new SessionServiceImpl(
                properties,
                sessionRepository,
                mock(SessionBlackRepository.class),
                mock(MusicVoteRepository.class),
                new SessionOutboundMailboxes(
                        Runnable::run, new OutboundMailboxProperties()),
                mock(ClientConnectionRegistry.class));
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("remoteAddress", "192.0.2.1");
        when(session.getId()).thenReturn("session");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);
        long before = System.currentTimeMillis();

        service.putSession(session, "room");

        long after = System.currentTimeMillis();
        ArgumentCaptor<User> stored = ArgumentCaptor.forClass(User.class);
        verify(sessionRepository).setSession(stored.capture(), eq("room"));
        assertTrue(stored.getValue().getJoinedAt() >= before);
        assertTrue(stored.getValue().getJoinedAt() <= after);
    }

    @Test
    void settingNameIgnoresSessionThatAlreadyExpired() {
        SessionRepository sessionRepository = mock(SessionRepository.class);
        when(sessionRepository.getSession("expired", "room")).thenReturn(null);
        SessionServiceImpl service = new SessionServiceImpl(
                new JusicProperties(),
                sessionRepository,
                mock(SessionBlackRepository.class),
                mock(MusicVoteRepository.class),
                new SessionOutboundMailboxes(
                        Runnable::run, new OutboundMailboxProperties()),
                mock(ClientConnectionRegistry.class));

        assertDoesNotThrow(() ->
                service.settingName("expired", "Alice", "room"));

        verify(sessionRepository, never()).setSession(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }
}

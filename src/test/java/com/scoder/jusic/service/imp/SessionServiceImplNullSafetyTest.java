package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.ClientConnectionRegistry;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceImplNullSafetyTest {

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

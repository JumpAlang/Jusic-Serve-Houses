package com.scoder.jusic.job;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.PresenceProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionPresenceJobTest {

    @Test
    void disabledPresenceDoesNotScanSessionsOrRooms() {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession session = session("session-a", true);
        jusicProperties.getSessions("house-a").put(session.getId(), session);
        HouseContainer houseContainer = mock(HouseContainer.class);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);

        SessionPresenceJob job = job(
                jusicProperties,
                houseContainer,
                sessionRepository,
                sessionService,
                false
        );
        job.refreshLocalPresence();
        job.pruneRedisGhosts();

        verify(session, never()).isOpen();
        verifyNoInteractions(houseContainer, sessionRepository, sessionService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void openLocalSessionsRenewTheirPresenceInOneBatch() {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession first = session("session-a", true);
        WebSocketSession second = session("session-b", true);
        jusicProperties.getSessions("house-a").put(first.getId(), first);
        jusicProperties.getSessions("house-b").put(second.getId(), second);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);

        job(jusicProperties, mock(HouseContainer.class), sessionRepository,
                sessionService, true).refreshLocalPresence();

        ArgumentCaptor<Map<String, ? extends Collection<String>>> captor =
                ArgumentCaptor.forClass(Map.class);
        verify(sessionRepository).refreshPresence(captor.capture());
        assertTrue(captor.getValue().get("house-a").contains("session-a"));
        assertTrue(captor.getValue().get("house-b").contains("session-b"));
        verify(sessionService, never())
                .clearSession(any(WebSocketSession.class), any(String.class));
    }

    @Test
    void closedLocalSessionIsCleanedWithoutRefreshingItsLease() {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession session = session("session-a", false);
        jusicProperties.getSessions("house-a").put(session.getId(), session);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);

        job(jusicProperties, mock(HouseContainer.class), sessionRepository,
                sessionService, true).refreshLocalPresence();

        verify(sessionService).clearSession(session, "house-a");
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void missingUserHashMustRepeatBeforeConnectionIsFailedClosed()
            throws Exception {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession session = session("session-a", true);
        jusicProperties.getSessions("house-a").put(session.getId(), session);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);
        when(sessionRepository.refreshPresence(anyMap()))
                .thenReturn(Collections.singletonMap(
                        "house-a", Collections.singletonList("session-a")));
        SessionPresenceJob job = job(jusicProperties,
                mock(HouseContainer.class), sessionRepository,
                sessionService, true);

        job.refreshLocalPresence();

        verify(sessionService, never()).clearSession(session, "house-a");
        verify(session, never()).close(any(CloseStatus.class));

        job.refreshLocalPresence();

        verify(sessionService).clearSession(session, "house-a");
        verify(session).close(CloseStatus.SESSION_NOT_RELIABLE);
        verify(sessionRepository, times(2)).refreshPresence(anyMap());
    }

    @Test
    void largeMissingListsAreConvertedBeforeMembershipChecks() {
        JusicProperties jusicProperties = new JusicProperties();
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);
        CountingContainsList missing = new CountingContainsList();
        for (int i = 0; i < 500; i++) {
            String sessionId = "session-" + i;
            WebSocketSession session = session(sessionId, true);
            jusicProperties.getSessions("house-a").put(sessionId, session);
            missing.add(sessionId);
        }
        when(sessionRepository.refreshPresence(anyMap()))
                .thenReturn(Collections.singletonMap("house-a", missing));

        job(jusicProperties, mock(HouseContainer.class), sessionRepository,
                sessionService, true).refreshLocalPresence();

        assertEquals(0, missing.containsCalls.get(),
                "membership checks must use the converted HashSet");
        verify(sessionService, never())
                .clearSession(any(WebSocketSession.class), any(String.class));
    }

    @Test
    void redisFailureDoesNotDiscardAHealthyLocalConnection() {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession session = session("session-a", true);
        jusicProperties.getSessions("house-a").put(session.getId(), session);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionService sessionService = mock(SessionService.class);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(sessionRepository).refreshPresence(anyMap());

        job(jusicProperties, mock(HouseContainer.class), sessionRepository,
                sessionService, true).refreshLocalPresence();

        verify(sessionService, never())
                .clearSession(any(WebSocketSession.class), any(String.class));
    }

    @Test
    void pruningVisitsEveryValidRoomAndIsolatesOneRoomFailure() {
        JusicProperties jusicProperties = new JusicProperties();
        WebSocketSession protectedSession = session("session-a", true);
        jusicProperties.getSessions("house-a").put(
                protectedSession.getId(), protectedSession);
        House noId = new House();
        House first = House.builder().id("house-a").build();
        House second = House.builder().id("house-b").build();
        HouseContainer houseContainer = mock(HouseContainer.class);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        when(houseContainer.getHouses()).thenReturn(
                new CopyOnWriteArrayList<>(Arrays.asList(null, noId, first, second))
        );
        when(sessionRepository.pruneExpiredPresence(
                eq("house-a"), anyCollection()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        when(sessionRepository.pruneExpiredPresence(
                eq("house-b"), anyCollection()))
                .thenReturn(2L);
        when(sessionRepository.findPresenceHouseIds())
                .thenReturn(Collections.singleton("crashed-temporary-house"));
        when(houseContainer.destroyRedisIfUnknownAndEmpty(
                "crashed-temporary-house")).thenReturn(true);

        job(jusicProperties, houseContainer, sessionRepository,
                mock(SessionService.class), true).pruneRedisGhosts();

        verify(sessionRepository).pruneExpiredPresence(
                eq("house-a"), argThat(ids -> ids.contains("session-a")));
        verify(sessionRepository).pruneExpiredPresence(
                eq("house-b"), argThat(Collection::isEmpty));
        verify(sessionRepository).pruneExpiredPresence(
                eq("crashed-temporary-house"), argThat(Collection::isEmpty));
        verify(houseContainer).destroyRedisIfUnknownAndEmpty(
                "crashed-temporary-house");
    }

    private static SessionPresenceJob job(JusicProperties jusicProperties,
                                          HouseContainer houseContainer,
                                          SessionRepository sessionRepository,
                                          SessionService sessionService,
                                          boolean enabled) {
        PresenceProperties properties = new PresenceProperties();
        properties.setEnabled(enabled);
        return new SessionPresenceJob(jusicProperties, houseContainer,
                sessionRepository, sessionService, properties);
    }

    private static WebSocketSession session(String id, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(open);
        return session;
    }

    private static final class CountingContainsList extends ArrayList<String> {
        private final AtomicInteger containsCalls = new AtomicInteger();

        @Override
        public boolean contains(Object value) {
            containsCalls.incrementAndGet();
            return super.contains(value);
        }
    }
}

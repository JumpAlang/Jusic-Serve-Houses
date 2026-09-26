package com.scoder.jusic.handler;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.ClientConnectionRegistry;
import com.scoder.jusic.configuration.ConnectionAdmissionException;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.interceptor.ClientIdentityFilter;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.Online;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JusicWebSocketHandlerLifecycleTest {

    @Test
    void firstSubscriptionReplaysMusicAndPickAfterSnapshotIsPrepared()
            throws Exception {
        Fixture fixture = fixture("room-ready-after", house("room-ready-after", true));

        fixture.handler.afterConnectionEstablished(fixture.session);
        fixture.handler.clientMessageHandlerReady(
                "session-1", "room-ready-after", fixture.attributes);
        fixture.handler.clientMessageHandlerReady(
                "session-1", "room-ready-after", fixture.attributes);

        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.MUSIC), any(), eq("room-ready-after"));
        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.PICK), any(), eq("room-ready-after"));
        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.ROOM_STATE), any(), eq("room-ready-after"));
    }

    @Test
    void snapshotIsReplayedWhenSubscriptionWinsInitializationRace()
            throws Exception {
        Fixture fixture = fixture("room-ready-before", house("room-ready-before", true));

        fixture.handler.clientMessageHandlerReady(
                "session-1", "room-ready-before", fixture.attributes);
        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.MUSIC), any(), eq("room-ready-before"));
        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.PICK), any(), eq("room-ready-before"));
        verify(fixture.sessionService, times(1)).send(
                eq("session-1"), eq(MessageType.ROOM_STATE), any(), eq("room-ready-before"));
    }

    @Test
    void capacityRejectionClosesWith1013AndStopsInitialization() throws Exception {
        Fixture fixture = fixture("room-capacity", house("room-capacity", true));
        ConnectionAdmissionException rejection = mock(ConnectionAdmissionException.class);
        when(rejection.getReason()).thenReturn(ConnectionAdmissionException.Reason.NODE_LIMIT);
        when(fixture.sessionService.putSession(fixture.session, "room-capacity"))
                .thenThrow(rejection);

        fixture.handler.afterConnectionEstablished(fixture.session);

        ArgumentCaptor<CloseStatus> closeStatus = ArgumentCaptor.forClass(CloseStatus.class);
        verify(fixture.session).close(closeStatus.capture());
        assertEquals(1013, closeStatus.getValue().getCode());
        assertEquals("connection capacity reached", closeStatus.getValue().getReason());
        assertEquals(Boolean.TRUE, fixture.attributes.get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE));
        verify(fixture.sessionService).putSession(fixture.session, "room-capacity");
        verify(fixture.sessionService, never()).send(
                any(WebSocketSession.class), any(MessageType.class), any());
        verify(fixture.playingRepository, never()).getPlaying(any());
        verify(fixture.musicService, never()).getPickList(any());
        assertNull(fixture.clientConnectionRegistry.current("session-1"));
    }

    @Test
    void runtimeAdmissionFailureReleasesReservationAndClosesWithServerError()
            throws Exception {
        Fixture fixture = fixture(
                "room-admission-failure", house("room-admission-failure", true));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(fixture.sessionService)
                .putSession(fixture.session, "room-admission-failure");

        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.session).close(CloseStatus.SERVER_ERROR);
        assertNull(fixture.clientConnectionRegistry.current("session-1"));
        verify(fixture.sessionService, never()).send(
                any(WebSocketSession.class), any(MessageType.class), any());
    }

    @Test
    void duplicateBrowserConnectionIsRejectedBeforeAdmissionAndKeepsFirstHealthy()
            throws Exception {
        String houseId = "room-duplicate-rejection";
        String clientId = "stable-client";
        Fixture fixture = fixture(houseId, house(houseId, true));
        ClientConnectionRegistry registry = new ClientConnectionRegistry();
        ReflectionTestUtils.setField(
                fixture.handler, "clientConnectionRegistry", registry);
        fixture.attributes.put(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);

        Map<String, Object> oldAttributes = new HashMap<>();
        oldAttributes.put("houseId", houseId);
        oldAttributes.put(ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);
        WebSocketSession oldSession = mock(WebSocketSession.class);
        when(oldSession.getAttributes()).thenReturn(oldAttributes);
        when(oldSession.getId()).thenReturn("old-session");
        when(oldSession.isOpen()).thenReturn(true);
        registry.bind(clientId, oldSession);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandler decorator = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(
                decorator, "jusicWebSocketHandlerAsync", fixture.handler);

        decorator.afterConnectionEstablished(fixture.session);

        assertSame(oldSession, registry.current(clientId));
        verify(delegate, never()).afterConnectionEstablished(fixture.session);
        verify(fixture.sessionService, never())
                .putSession(fixture.session, houseId);
        verify(fixture.sessionService, never())
                .clearSession(oldSession, houseId);
        verify(oldSession, never()).close(any(CloseStatus.class));
        ArgumentCaptor<CloseStatus> rejectedClose =
                ArgumentCaptor.forClass(CloseStatus.class);
        verify(fixture.session).close(rejectedClose.capture());
        assertEquals(JusicWebSocketHandlerAsync.DUPLICATE_CLIENT_CLOSE_CODE,
                rejectedClose.getValue().getCode());
        assertEquals(JusicWebSocketHandlerAsync.DUPLICATE_CLIENT_CLOSE_REASON,
                rejectedClose.getValue().getReason());

        decorator.afterConnectionClosed(
                fixture.session, rejectedClose.getValue());

        verify(delegate, never()).afterConnectionClosed(
                fixture.session, rejectedClose.getValue());
        verify(fixture.sessionService, never())
                .clearSession(any(WebSocketSession.class), eq(houseId));
        verify(fixture.sessionService, never())
                .send(eq(MessageType.ONLINE), any(), eq(houseId));
        verify(fixture.houseContainer, never())
                .destroyIfTemporaryAndEmpty(houseId);
        assertFalse(Boolean.TRUE.equals(oldAttributes.get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE)));
        assertSame(oldSession, registry.current(clientId));
    }

    @Test
    void targetHouseLifecycleLockBlocksDestroyWhileRegistrationIsInFlight() throws Exception {
        Fixture fixture = fixture("room-lock", house("room-lock", true));
        CountDownLatch insertionStarted = new CountDownLatch(1);
        CountDownLatch releaseInsertion = new CountDownLatch(1);
        CountDownLatch destroyAttemptingLock = new CountDownLatch(1);
        CountDownLatch destroyPassedLock = new CountDownLatch(1);
        AtomicReference<Throwable> establishFailure = new AtomicReference<>();
        AtomicReference<Throwable> destroyFailure = new AtomicReference<>();
        doAnswer(invocation -> {
            insertionStarted.countDown();
            assertTrue(releaseInsertion.await(2, TimeUnit.SECONDS));
            return null;
        }).when(fixture.sessionService).putSession(fixture.session, "room-lock");

        Thread establish = new Thread(() -> {
            try {
                fixture.handler.afterConnectionEstablished(fixture.session);
            } catch (Throwable ex) {
                establishFailure.set(ex);
            }
        });
        Thread destroy = new Thread(() -> {
            ReentrantLock roomLock = fixture.houseOperationLocks.getLock("room-lock");
            destroyAttemptingLock.countDown();
            roomLock.lock();
            try {
                destroyPassedLock.countDown();
            } catch (Throwable ex) {
                destroyFailure.set(ex);
            } finally {
                roomLock.unlock();
            }
        });

        establish.start();
        assertTrue(insertionStarted.await(2, TimeUnit.SECONDS));
        destroy.start();
        assertTrue(destroyAttemptingLock.await(2, TimeUnit.SECONDS));
        assertFalse(destroyPassedLock.await(200, TimeUnit.MILLISECONDS));
        releaseInsertion.countDown();
        assertTrue(destroyPassedLock.await(2, TimeUnit.SECONDS));
        establish.join(3_000L);
        destroy.join(3_000L);

        assertFalse(establish.isAlive());
        assertFalse(destroy.isAlive());
        assertNull(establishFailure.get());
        assertNull(destroyFailure.get());
        verify(fixture.sessionService).putSession(fixture.session, "room-lock");
    }

    @Test
    void normalRegistrationStillCompletesInitialization() throws Exception {
        Fixture fixture = fixture("room-normal", house("room-normal", true));

        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.sessionService).putSession(fixture.session, "room-normal");
        verify(fixture.sessionService).send(
                eq(fixture.session), eq(MessageType.NOTICE), any());
        verify(fixture.sessionService).send(
                eq(MessageType.ONLINE), any(), eq("room-normal"));
        verify(fixture.playingRepository).getPlaying("room-normal");
        verify(fixture.musicService).getPickList("room-normal");
        verify(fixture.session, never()).close(any(CloseStatus.class));
        assertFalse(Boolean.TRUE.equals(fixture.attributes.get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE)));
    }

    @Test
    void snapshotFailureCleansRegisteredSessionAndClosesConnection()
            throws Exception {
        Fixture fixture = fixture(
                "room-snapshot-failure", house("room-snapshot-failure", true));
        when(fixture.playingRepository.getPlaying("room-snapshot-failure"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.sessionService).clearSession(
                fixture.session, "room-snapshot-failure");
        verify(fixture.session).close(CloseStatus.SERVER_ERROR);
        assertNull(fixture.clientConnectionRegistry.current("session-1"));
        assertEquals(Boolean.TRUE, fixture.attributes.get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE));
        verify(fixture.musicService, never()).getPickList(any());
    }

    @Test
    void secondConnectionCannotReplaceOrCleanFirstRegisteredConnection()
            throws Exception {
        String houseId = "room-browser-first-wins";
        String clientId = "stable-browser-id";
        Fixture fixture = fixture(houseId, house(houseId, true));
        JusicProperties membership = new JusicProperties();
        ClientConnectionRegistry registry = new ClientConnectionRegistry();
        ReflectionTestUtils.setField(fixture.handler, "musicBar", membership);
        ReflectionTestUtils.setField(
                fixture.handler, "clientConnectionRegistry", registry);

        fixture.attributes.put(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);
        doAnswer(invocation -> {
            WebSocketSession registered = invocation.getArgument(0);
            String targetHouseId = invocation.getArgument(1);
            membership.getSession().move(
                    registered, targetHouseId, "127.0.0.1");
            return null;
        }).when(fixture.sessionService).putSession(
                any(WebSocketSession.class), eq(houseId));
        doAnswer(invocation -> {
            WebSocketSession removed = invocation.getArgument(0);
            membership.getSession().remove(removed);
            return null;
        }).when(fixture.sessionService).clearSession(
                any(WebSocketSession.class), eq(houseId));
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandler decorator = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(
                decorator, "jusicWebSocketHandlerAsync", fixture.handler);

        decorator.afterConnectionEstablished(fixture.session);

        assertSame(fixture.session, registry.current(clientId));
        assertEquals(1, membership.getSessionCount(houseId));

        Map<String, Object> newcomerAttributes = new HashMap<>();
        newcomerAttributes.put("houseId", houseId);
        newcomerAttributes.put(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);
        WebSocketSession newcomer = mock(WebSocketSession.class);
        when(newcomer.getAttributes()).thenReturn(newcomerAttributes);
        when(newcomer.getId()).thenReturn("session-2");
        when(newcomer.isOpen()).thenReturn(true);

        decorator.afterConnectionEstablished(newcomer);

        verify(delegate).afterConnectionEstablished(any(WebSocketSession.class));
        verify(delegate, never()).afterConnectionEstablished(newcomer);
        verify(fixture.sessionService, never())
                .clearSession(any(WebSocketSession.class), eq(houseId));
        verify(fixture.session, never()).close(any(CloseStatus.class));
        ArgumentCaptor<CloseStatus> duplicateClose =
                ArgumentCaptor.forClass(CloseStatus.class);
        verify(newcomer).close(duplicateClose.capture());
        assertEquals(JusicWebSocketHandlerAsync.DUPLICATE_CLIENT_CLOSE_CODE,
                duplicateClose.getValue().getCode());
        assertEquals(JusicWebSocketHandlerAsync.DUPLICATE_CLIENT_CLOSE_REASON,
                duplicateClose.getValue().getReason());
        assertSame(fixture.session, registry.current(clientId));
        assertSame(fixture.session,
                membership.getSession().getSessionIfPresent(
                        houseId, fixture.session.getId()));
        assertNull(membership.getSession().getSessionIfPresent(
                houseId, newcomer.getId()));
        assertEquals(1, membership.getSessionCount(houseId));
        verify(fixture.sessionService, times(1)).putSession(
                any(WebSocketSession.class), eq(houseId));

        decorator.afterConnectionClosed(
                newcomer, duplicateClose.getValue());

        verify(delegate, never()).afterConnectionClosed(
                newcomer, duplicateClose.getValue());
        verify(fixture.sessionService, never())
                .clearSession(any(WebSocketSession.class), eq(houseId));
        assertSame(fixture.session, registry.current(clientId));
        assertSame(fixture.session,
                membership.getSession().getSessionIfPresent(
                        houseId, fixture.session.getId()));
        assertEquals(1, membership.getSessionCount(houseId));
    }

    @Test
    void onlineBroadcastUsesCurrentMembershipCount() throws Exception {
        Fixture fixture = fixture("room-latest-count",
                house("room-latest-count", true));
        JusicProperties membership = org.mockito.Mockito.spy(
                new JusicProperties());
        membership.getSession().register(
                fixture.session, "room-latest-count", "127.0.0.1");
        WebSocketSession secondSession = mock(WebSocketSession.class);
        when(secondSession.getId()).thenReturn("session-2");
        membership.getSession().register(
                secondSession, "room-latest-count", "127.0.0.2");
        ReflectionTestUtils.setField(fixture.handler, "musicBar", membership);

        fixture.handler.afterConnectionEstablished(fixture.session);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(fixture.sessionService).send(
                eq(MessageType.ONLINE), payload.capture(),
                eq("room-latest-count"));
        Response<?> response = (Response<?>) payload.getValue();
        Online online = (Online) response.getData();
        assertEquals(2, online.getCount());
        verify(membership, org.mockito.Mockito.times(2))
                .getSessionCount("room-latest-count");
    }

    @Test
    void emptyRoomInitializationDoesNotPollForPlayingMusic() throws Exception {
        Fixture fixture = fixture("room-empty", house("room-empty", true));
        when(fixture.playingRepository.getPlaying("room-empty"))
                .thenReturn(null);

        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.playingRepository, times(1))
                .getPlaying("room-empty");
        verify(fixture.musicService).getPickList("room-empty");
    }

    @Test
    void closeTombstonePreventsAQueuedEstablishFromCreatingZombieSession() throws Exception {
        Fixture fixture = fixture("room-1", house("room-1", true));

        fixture.handler.markConnectionClosed(fixture.session);
        fixture.handler.afterConnectionEstablished(fixture.session);

        verify(fixture.sessionService, never()).putSession(any(), any());
    }

    @Test
    void fallbackReassignsHouseAndMissingRemoteAddressDoesNotFailInitialization() throws Exception {
        House fallback = house(JusicProperties.HOUSE_DEFAULT_ID, true);
        Fixture fixture = fixture("missing", null);
        when(fixture.houseContainer.get(JusicProperties.HOUSE_DEFAULT_ID))
                .thenReturn(fallback);

        fixture.handler.afterConnectionEstablished(fixture.session);

        assertEquals(JusicProperties.HOUSE_DEFAULT_ID,
                fixture.attributes.get("houseId"));
        verify(fixture.sessionService).putSession(
                fixture.session, JusicProperties.HOUSE_DEFAULT_ID);
    }

    @Test
    void closeWaitsForAnInFlightInsertionAndThenRemovesIt() throws Exception {
        Fixture fixture = fixture("room-race", house("room-race", true));
        CountDownLatch insertionStarted = new CountDownLatch(1);
        CountDownLatch releaseInsertion = new CountDownLatch(1);
        doAnswer(invocation -> {
            insertionStarted.countDown();
            assertTrue(releaseInsertion.await(2, TimeUnit.SECONDS));
            return null;
        }).when(fixture.sessionService).putSession(fixture.session, "room-race");
        AtomicReference<Throwable> establishFailure = new AtomicReference<>();
        AtomicReference<Throwable> closeFailure = new AtomicReference<>();

        Thread establish = new Thread(() -> {
            try {
                fixture.handler.afterConnectionEstablished(fixture.session);
            } catch (Throwable ex) {
                establishFailure.set(ex);
            }
        });
        Thread close = new Thread(() -> {
            try {
                fixture.handler.afterConnectionClosed(
                        fixture.session, CloseStatus.NORMAL);
            } catch (Throwable ex) {
                closeFailure.set(ex);
            }
        });

        establish.start();
        assertTrue(insertionStarted.await(2, TimeUnit.SECONDS));
        close.start();
        releaseInsertion.countDown();
        establish.join(2_000L);
        close.join(2_000L);

        assertFalse(establish.isAlive());
        assertFalse(close.isAlive());
        assertNull(establishFailure.get());
        assertNull(closeFailure.get());
        InOrder order = inOrder(fixture.sessionService);
        order.verify(fixture.sessionService).putSession(fixture.session, "room-race");
        order.verify(fixture.sessionService).clearSession(fixture.session, "room-race");
        assertEquals(Boolean.TRUE, fixture.attributes.get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE));
    }

    @Test
    void decoratorMarksCloseBeforeDelegateAndUsesSynchronousCleanup() throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(handler, "jusicWebSocketHandlerAsync", lifecycle);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        InOrder order = inOrder(lifecycle, delegate);
        order.verify(lifecycle).markConnectionClosed(session);
        order.verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
        order.verify(lifecycle).afterConnectionClosed(session, CloseStatus.NORMAL);
    }

    @Test
    void decoratorReservesBeforeDelegateAndInitializesOnlyAfterDelegate()
            throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(lifecycle.tryReserveConnection(session)).thenReturn(true);
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(
                handler, "jusicWebSocketHandlerAsync", lifecycle);

        handler.afterConnectionEstablished(session);

        InOrder order = inOrder(lifecycle, delegate);
        order.verify(lifecycle).tryReserveConnection(session);
        ArgumentCaptor<WebSocketSession> delegateSession =
                ArgumentCaptor.forClass(WebSocketSession.class);
        order.verify(delegate).afterConnectionEstablished(
                delegateSession.capture());
        order.verify(lifecycle).afterConnectionEstablished(session);
        assertSame(delegateSession.getValue(),
                SerializedWebSocketSession.outboundSession(session));
    }

    @Test
    void duplicateReservationStopsBeforeDelegate() throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(lifecycle.tryReserveConnection(session)).thenReturn(false);
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(
                handler, "jusicWebSocketHandlerAsync", lifecycle);

        handler.afterConnectionEstablished(session);

        verify(lifecycle).tryReserveConnection(session);
        verify(delegate, never()).afterConnectionEstablished(session);
        verify(lifecycle, never()).afterConnectionEstablished(session);
    }

    @Test
    void delegateFailureConditionallyReleasesReservation() throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        RuntimeException failure = new RuntimeException("delegate failed");
        when(lifecycle.tryReserveConnection(session)).thenReturn(true);
        when(session.isOpen()).thenReturn(true);
        doThrow(failure).when(delegate).afterConnectionEstablished(
                any(WebSocketSession.class));
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(
                handler, "jusicWebSocketHandlerAsync", lifecycle);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> handler.afterConnectionEstablished(session));

        assertSame(failure, thrown);
        InOrder order = inOrder(lifecycle, delegate, session);
        order.verify(lifecycle).tryReserveConnection(session);
        order.verify(delegate).afterConnectionEstablished(
                any(WebSocketSession.class));
        order.verify(lifecycle).releaseConnectionReservation(session);
        order.verify(lifecycle).markConnectionClosed(session);
        order.verify(session).close(CloseStatus.SERVER_ERROR);
        verify(lifecycle, never()).afterConnectionEstablished(session);
    }

    @Test
    void duplicateCloseCallbacksCleanApplicationPresenceOnlyOnce() throws Exception {
        Fixture fixture = fixture("room-close-once", house("room-close-once", true));

        fixture.handler.afterConnectionClosed(fixture.session, CloseStatus.NORMAL);
        fixture.handler.afterConnectionClosed(fixture.session, CloseStatus.NORMAL);

        verify(fixture.sessionService).clearSession(
                fixture.session, "room-close-once");
    }

    @Test
    void closeBeforeRoomAdmissionReleasesBrowserReservation() throws Exception {
        Fixture fixture = fixture("room-early-close", house("room-early-close", true));
        fixture.attributes.remove("houseId");

        fixture.handler.afterConnectionClosed(fixture.session, CloseStatus.NORMAL);

        assertEquals(0, fixture.clientConnectionRegistry.size());
        verify(fixture.sessionService, never()).clearSession(
                any(WebSocketSession.class), any(String.class));
    }

    @Test
    void transportErrorMarksAndCleansWithoutWaitingForAnotherCloseCallback() throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        RuntimeException failure = new RuntimeException("transport failed");
        when(session.isOpen()).thenReturn(true);
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(handler, "jusicWebSocketHandlerAsync", lifecycle);

        handler.handleTransportError(session, failure);

        InOrder order = inOrder(lifecycle, delegate, session);
        order.verify(lifecycle).markConnectionClosed(session);
        order.verify(delegate).handleTransportError(session, failure);
        order.verify(lifecycle).afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
        order.verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void rejectedInitializationClosesInsteadOfRunningOnWebSocketCaller() throws Exception {
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        JusicWebSocketHandlerAsync lifecycle = mock(JusicWebSocketHandlerAsync.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("rejected-session");
        when(lifecycle.tryReserveConnection(session)).thenReturn(true);
        doThrow(new TaskRejectedException("full"))
                .when(lifecycle).afterConnectionEstablished(session);
        JusicWebSocketHandler handler = new JusicWebSocketHandler(delegate);
        ReflectionTestUtils.setField(handler, "jusicWebSocketHandlerAsync", lifecycle);

        handler.afterConnectionEstablished(session);

        InOrder order = inOrder(delegate, lifecycle, session);
        order.verify(lifecycle).tryReserveConnection(session);
        order.verify(delegate).afterConnectionEstablished(
                any(WebSocketSession.class));
        order.verify(lifecycle).afterConnectionEstablished(session);
        order.verify(lifecycle).releaseConnectionReservation(session);
        order.verify(lifecycle).markConnectionClosed(session);
        order.verify(session).close(any(CloseStatus.class));
    }

    private Fixture fixture(String requestedHouseId, House requestedHouse) {
        Fixture fixture = new Fixture();
        fixture.handler = new JusicWebSocketHandlerAsync();
        fixture.sessionService = mock(SessionService.class);
        fixture.houseContainer = mock(HouseContainer.class);
        fixture.houseOperationLocks = new HouseOperationLocks();
        fixture.attributes = new HashMap<>();
        fixture.attributes.put("houseId", requestedHouseId);
        fixture.attributes.put(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, "session-1");
        fixture.session = mock(WebSocketSession.class);
        when(fixture.session.getAttributes()).thenReturn(fixture.attributes);
        when(fixture.session.getId()).thenReturn("session-1");
        when(fixture.session.isOpen()).thenReturn(true);
        when(fixture.houseContainer.get(requestedHouseId)).thenReturn(requestedHouse);

        JusicProperties properties = new JusicProperties();
        fixture.playingRepository = mock(MusicPlayingRepository.class);
        fixture.musicService = mock(MusicService.class);
        ConfigService configService = mock(ConfigService.class);
        Music playing = new Music();
        playing.setPushTime(System.currentTimeMillis());
        playing.setDuration(60_000L);
        when(fixture.playingRepository.getPlaying(any())).thenReturn(playing);
        when(fixture.musicService.getPickList(any())).thenReturn(new LinkedList<>());

        ReflectionTestUtils.setField(fixture.handler, "musicBar", properties);
        ReflectionTestUtils.setField(fixture.handler, "musicPlayingRepository", fixture.playingRepository);
        ReflectionTestUtils.setField(fixture.handler, "sessionService", fixture.sessionService);
        ReflectionTestUtils.setField(fixture.handler, "musicService", fixture.musicService);
        ReflectionTestUtils.setField(fixture.handler, "configService", configService);
        ReflectionTestUtils.setField(fixture.handler, "houseContainer", fixture.houseContainer);
        ReflectionTestUtils.setField(fixture.handler, "houseOperationLocks", fixture.houseOperationLocks);
        fixture.clientConnectionRegistry = new ClientConnectionRegistry();
        fixture.clientConnectionRegistry.tryReserve(
                "session-1", fixture.session);
        ReflectionTestUtils.setField(fixture.handler, "clientConnectionRegistry",
                fixture.clientConnectionRegistry);
        return fixture;
    }

    private House house(String id, boolean enabled) {
        House house = new House();
        house.setId(id);
        house.setEnableStatus(enabled);
        return house;
    }

    private static final class Fixture {
        private JusicWebSocketHandlerAsync handler;
        private SessionService sessionService;
        private HouseContainer houseContainer;
        private HouseOperationLocks houseOperationLocks;
        private ClientConnectionRegistry clientConnectionRegistry;
        private MusicPlayingRepository playingRepository;
        private MusicService musicService;
        private WebSocketSession session;
        private Map<String, Object> attributes;
    }
}

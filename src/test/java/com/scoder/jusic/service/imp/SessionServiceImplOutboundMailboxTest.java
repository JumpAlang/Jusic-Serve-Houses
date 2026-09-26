package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceImplOutboundMailboxTest {

    @Test
    void preservesFifoAndSchedulesOnlyOneDrainPerSession() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
        WebSocketSession session = openSession("session-fifo", "house-1");

        assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                mailboxes.dispatch(session, new TextMessage("first")));
        assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                mailboxes.dispatch(session, new TextMessage("second")));
        assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                mailboxes.dispatch(session, new TextMessage("third")));
        assertEquals(1, executor.size(), "one session must have only one scheduled drain");

        executor.runNext();

        ArgumentCaptor<TextMessage> messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(3)).sendMessage(messages.capture());
        List<String> payloads = new ArrayList<>();
        for (TextMessage message : messages.getAllValues()) {
            payloads.add(message.getPayload());
        }
        assertEquals(asList("first", "second", "third"), payloads);
        assertEquals(0, executor.size());
    }

    @Test
    void neverRunsTwoWritersForTheSameSession() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
            WebSocketSession session = openSession("session-single-writer", "house-1");
            AtomicInteger activeWriters = new AtomicInteger();
            AtomicInteger maximumActiveWriters = new AtomicInteger();
            CountDownLatch firstWriteStarted = new CountDownLatch(1);
            CountDownLatch releaseFirstWrite = new CountDownLatch(1);
            CountDownLatch allWritesCompleted = new CountDownLatch(3);

            doAnswer(invocation -> {
                int active = activeWriters.incrementAndGet();
                maximumActiveWriters.accumulateAndGet(active, Math::max);
                if (firstWriteStarted.getCount() > 0) {
                    firstWriteStarted.countDown();
                    assertTrue(releaseFirstWrite.await(2, TimeUnit.SECONDS));
                }
                activeWriters.decrementAndGet();
                allWritesCompleted.countDown();
                return null;
            }).when(session).sendMessage(any(TextMessage.class));

            mailboxes.dispatch(session, new TextMessage("first"));
            assertTrue(firstWriteStarted.await(2, TimeUnit.SECONDS));
            mailboxes.dispatch(session, new TextMessage("second"));
            mailboxes.dispatch(session, new TextMessage("third"));
            releaseFirstWrite.countDown();

            assertTrue(allWritesCompleted.await(2, TimeUnit.SECONDS));
            assertEquals(1, maximumActiveWriters.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void messageCountOverflowClosesAndRemovesSlowSession() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 2, 16_384);
        Fixture fixture = fixture(mailboxes, "session-overflow", "house-1");

        fixture.service.send(fixture.session, MessageType.NOTICE, "first");
        fixture.service.send(fixture.session, MessageType.NOTICE, "second");
        fixture.service.send(fixture.session, MessageType.NOTICE, "overflow");

        verify(fixture.session).close(CloseStatus.SERVICE_OVERLOAD);
        verify(fixture.sessionRepository).removeSession("session-overflow", "house-1");
        assertFalse(fixture.properties.getSessions("house-1").containsKey("session-overflow"));
        assertEquals(0, mailboxes.mailboxCount());

        executor.runNext();
        verify(fixture.session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void utf8ByteLimitIsEnforced() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 5);
        WebSocketSession session = openSession("session-bytes", "house-1");

        SessionOutboundMailboxes.DispatchResult result =
                mailboxes.dispatch(session, new TextMessage("音乐"));

        assertEquals(SessionOutboundMailboxes.DispatchResult.OVERFLOW, result,
                "two CJK characters consume six UTF-8 bytes");
        verify(session).close(CloseStatus.SERVICE_OVERLOAD);
        verify(session, never()).sendMessage(any(TextMessage.class));
        assertEquals(0, executor.size());
    }

    @Test
    void clearSessionDiscardsQueuedMessages() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
        Fixture fixture = fixture(mailboxes, "session-close", "house-1");

        fixture.service.send(fixture.session, MessageType.NOTICE, "queued");
        fixture.service.clearSession(fixture.session, "house-1");
        executor.runNext();

        verify(fixture.session, never()).sendMessage(any(TextMessage.class));
        verify(fixture.sessionRepository).removeSession("session-close", "house-1");
        assertEquals(0, mailboxes.mailboxCount());
        assertFalse(fixture.properties.getSessions("house-1").containsKey("session-close"));
    }

    @Test
    void clearSessionByIdAlsoDiscardsMailbox() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
        Fixture fixture = fixture(mailboxes, "session-by-id", "house-1");

        fixture.service.send(fixture.session, MessageType.NOTICE, "queued");
        WebSocketSession removed = fixture.service.clearSession("session-by-id", "house-1");
        executor.runNext();

        assertSame(fixture.session, removed);
        verify(fixture.session, never()).sendMessage(any(TextMessage.class));
        verify(fixture.sessionRepository).removeSession("session-by-id", "house-1");
        verify(fixture.musicVoteRepository).remove("session-by-id", "house-1");
        assertEquals(0, mailboxes.mailboxCount());
    }

    @Test
    void disconnectDuringWriteIsTreatedAsClosedSession() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
        WebSocketSession session = openSession("session-write-race", "house-1");
        when(session.isOpen()).thenReturn(true, true, true, false);
        doThrow(new IllegalStateException(
                "Message will not be sent because the WebSocket session has been closed"))
                .when(session).sendMessage(any(TextMessage.class));

        assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                mailboxes.dispatch(session, new TextMessage("online")));
        executor.runNext();

        assertEquals(0, mailboxes.mailboxCount());
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void redisSessionCleanupFailureDoesNotRetainLocalPresenceOrSkipVoteCleanup() {
        RecordingExecutor executor = new RecordingExecutor();
        SessionOutboundMailboxes mailboxes = mailboxes(executor, 10, 16_384);
        Fixture fixture = fixture(mailboxes, "session-redis-failure", "house-1");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(fixture.sessionRepository)
                .removeSession("session-redis-failure", "house-1");

        assertDoesNotThrow(() -> fixture.service.clearSession(
                fixture.session, "house-1"));

        assertFalse(fixture.properties.getSessions("house-1")
                .containsKey("session-redis-failure"));
        verify(fixture.musicVoteRepository)
                .remove("session-redis-failure", "house-1");
        assertEquals(0, mailboxes.mailboxCount());
    }

    @Test
    void executorRejectionClosesAndCleansSessionWithoutCallerRuns() throws Exception {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("full");
        };
        SessionOutboundMailboxes mailboxes = mailboxes(rejectingExecutor, 10, 16_384);
        Fixture fixture = fixture(mailboxes, "session-rejected", "house-1");

        fixture.service.send(fixture.session, MessageType.NOTICE, "must-not-run-inline");

        verify(fixture.session, never()).sendMessage(any(TextMessage.class));
        verify(fixture.session).close(CloseStatus.SERVICE_OVERLOAD);
        verify(fixture.sessionRepository).removeSession("session-rejected", "house-1");
        assertFalse(fixture.properties.getSessions("house-1").containsKey("session-rejected"));
        assertEquals(0, mailboxes.mailboxCount());
    }

    private static SessionOutboundMailboxes mailboxes(Executor executor,
                                                       int maxMessages,
                                                       long maxBytes) {
        OutboundMailboxProperties properties = new OutboundMailboxProperties();
        properties.setMaxPendingMessages(maxMessages);
        properties.setMaxPendingBytes(maxBytes);
        return new SessionOutboundMailboxes(executor, properties);
    }

    private static Fixture fixture(SessionOutboundMailboxes mailboxes,
                                   String sessionId,
                                   String houseId) {
        JusicProperties properties = new JusicProperties();
        SessionRepository sessionRepository = mock(SessionRepository.class);
        SessionBlackRepository blackRepository = mock(SessionBlackRepository.class);
        MusicVoteRepository musicVoteRepository = mock(MusicVoteRepository.class);
        WebSocketSession session = openSession(sessionId, houseId);
        properties.getSessions(houseId).put(sessionId, session);
        SessionServiceImpl service = new SessionServiceImpl(
                properties,
                sessionRepository,
                blackRepository,
                musicVoteRepository,
                mailboxes,
                mock(com.scoder.jusic.configuration.ClientConnectionRegistry.class)
        );
        return new Fixture(service, properties, sessionRepository, musicVoteRepository, session);
    }

    private static WebSocketSession openSession(String sessionId, String houseId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = Collections.singletonMap("houseId", houseId);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static List<String> asList(String first, String second, String third) {
        List<String> values = new ArrayList<>();
        values.add(first);
        values.add(second);
        values.add(third);
        return values;
    }

    private static final class RecordingExecutor implements Executor {
        private final List<Runnable> commands = new ArrayList<>();

        @Override
        public synchronized void execute(Runnable command) {
            commands.add(command);
        }

        private synchronized int size() {
            return commands.size();
        }

        private void runNext() {
            Runnable command;
            synchronized (this) {
                command = commands.isEmpty() ? null : commands.remove(0);
            }
            assertTrue(command != null, "expected a scheduled drain");
            command.run();
        }
    }

    private static final class Fixture {
        private final SessionServiceImpl service;
        private final JusicProperties properties;
        private final SessionRepository sessionRepository;
        private final MusicVoteRepository musicVoteRepository;
        private final WebSocketSession session;

        private Fixture(SessionServiceImpl service,
                        JusicProperties properties,
                        SessionRepository sessionRepository,
                        MusicVoteRepository musicVoteRepository,
                        WebSocketSession session) {
            this.service = service;
            this.properties = properties;
            this.sessionRepository = sessionRepository;
            this.musicVoteRepository = musicVoteRepository;
            this.session = session;
        }
    }
}

package com.scoder.jusic.handler;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SerializedWebSocketSessionTest {

    @Test
    void stompAndDirectPathsShareOnePhysicalWriteGate() throws Exception {
        WebSocketSession physicalSession = mock(WebSocketSession.class);
        when(physicalSession.getAttributes()).thenReturn(new HashMap<>());
        AtomicInteger activeWrites = new AtomicInteger();
        AtomicInteger maximumActiveWrites = new AtomicInteger();
        CountDownLatch firstWriteStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstWrite = new CountDownLatch(1);
        CountDownLatch writesCompleted = new CountDownLatch(2);
        doAnswer(invocation -> {
            int active = activeWrites.incrementAndGet();
            maximumActiveWrites.accumulateAndGet(active, Math::max);
            if (firstWriteStarted.getCount() > 0) {
                firstWriteStarted.countDown();
                assertTrue(releaseFirstWrite.await(2, TimeUnit.SECONDS));
            }
            activeWrites.decrementAndGet();
            writesCompleted.countDown();
            return null;
        }).when(physicalSession).sendMessage(any(TextMessage.class));

        WebSocketSession stompPath =
                SerializedWebSocketSession.wrap(physicalSession);
        WebSocketSession directPath =
                SerializedWebSocketSession.outboundSession(physicalSession);
        assertSame(stompPath, directPath);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.execute(() -> send(stompPath, "stomp"));
            assertTrue(firstWriteStarted.await(2, TimeUnit.SECONDS));
            executor.execute(() -> send(directPath, "direct"));
            releaseFirstWrite.countDown();

            assertTrue(writesCompleted.await(2, TimeUnit.SECONDS));
            assertEquals(1, maximumActiveWrites.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void send(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}

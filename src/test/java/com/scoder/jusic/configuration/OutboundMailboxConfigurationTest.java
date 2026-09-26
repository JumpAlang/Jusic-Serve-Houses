package com.scoder.jusic.configuration;

import com.scoder.jusic.service.imp.SessionOutboundMailboxes;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboundMailboxConfigurationTest {

    @Test
    void defaultBoundAcceptsFirstDrainForFiveThousandSessions() throws Exception {
        OutboundMailboxProperties properties = new OutboundMailboxProperties();
        assertTrue(properties.getExecutorQueueCapacity() >= 32_768,
                "the node-level queue default must cover the target connection tier");

        // Keep one writer occupied so all other first-drain tasks remain in the
        // real bounded executor queue during the assertion.
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        ThreadPoolTaskExecutor executor =
                new OutboundMailboxConfiguration().webSocketOutboundExecutor(properties);
        executor.initialize();

        CountDownLatch writerStarted = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        List<WebSocketSession> sessions = new ArrayList<>();
        try {
            SessionOutboundMailboxes mailboxes = new SessionOutboundMailboxes(executor, properties);
            WebSocketSession first = session("session-0");
            doAnswer(invocation -> {
                writerStarted.countDown();
                releaseWriter.await(5, TimeUnit.SECONDS);
                return null;
            }).when(first).sendMessage(any(TextMessage.class));
            sessions.add(first);

            assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                    mailboxes.dispatch(first, new TextMessage("first")));
            assertTrue(writerStarted.await(2, TimeUnit.SECONDS));

            for (int index = 1; index < 5_000; index++) {
                WebSocketSession session = session("session-" + index);
                sessions.add(session);
                assertEquals(SessionOutboundMailboxes.DispatchResult.ACCEPTED,
                        mailboxes.dispatch(session, new TextMessage("first")));
            }

            assertEquals(4_999, executor.getThreadPoolExecutor().getQueue().size());
            assertTrue(executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                    instanceof ThreadPoolExecutor.AbortPolicy);

            for (WebSocketSession session : sessions) {
                mailboxes.discard(session);
            }
        } finally {
            releaseWriter.countDown();
            executor.shutdown();
        }
    }

    private static WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}

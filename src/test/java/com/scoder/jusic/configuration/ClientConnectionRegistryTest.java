package com.scoder.jusic.configuration;

import com.scoder.jusic.interceptor.ClientIdentityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientConnectionRegistryTest {

    private static final String CLIENT_ID =
            "0123456789abcdef0123456789abcdef";

    private final ClientConnectionRegistry registry =
            new ClientConnectionRegistry();

    @Test
    void bindingSameClientAgainKeepsFirstSocketCurrentWithoutGrowingRegistry() {
        WebSocketSession first = session(CLIENT_ID);
        WebSocketSession newest = session(CLIENT_ID);

        registry.bind(CLIENT_ID, first);
        registry.bind(CLIENT_ID, newest);

        assertSame(first, registry.current(CLIENT_ID));
        assertEquals(1, registry.size());
    }

    @Test
    void rejectedSecondSocketCannotUnbindFirstSocket() {
        WebSocketSession first = session(CLIENT_ID);
        WebSocketSession rejected = session(CLIENT_ID);
        registry.bind(CLIENT_ID, first);
        registry.bind(CLIENT_ID, rejected);

        assertFalse(registry.unbind(rejected));

        assertSame(first, registry.current(CLIENT_ID));
        assertEquals(1, registry.size());
    }

    @Test
    void closingCurrentSocketConditionallyRemovesItsBinding() {
        WebSocketSession current = session(CLIENT_ID);
        registry.bind(CLIENT_ID, current);

        assertTrue(registry.unbind(current));

        assertEquals(0, registry.size());
        assertFalse(registry.unbind(current));
    }

    @Test
    void differentBrowserIdentitiesRemainIndependent() {
        String otherClientId = "fedcba9876543210fedcba9876543210";
        WebSocketSession first = session(CLIENT_ID);
        WebSocketSession second = session(otherClientId);

        registry.bind(CLIENT_ID, first);
        registry.bind(otherClientId, second);

        assertSame(first, registry.current(CLIENT_ID));
        assertSame(second, registry.current(otherClientId));
        assertEquals(2, registry.size());
    }

    @Test
    void concurrentReservationsAreFirstWinsAndReleaseIsIdentitySafe()
            throws Exception {
        WebSocketSession first = session(CLIENT_ID);
        WebSocketSession second = session(CLIENT_ID);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicBoolean firstWon = new AtomicBoolean();
        AtomicBoolean secondWon = new AtomicBoolean();
        Thread firstThread = new Thread(() -> {
            await(start);
            firstWon.set(registry.tryReserve(CLIENT_ID, first));
            done.countDown();
        });
        Thread secondThread = new Thread(() -> {
            await(start);
            secondWon.set(registry.tryReserve(CLIENT_ID, second));
            done.countDown();
        });

        firstThread.start();
        secondThread.start();
        start.countDown();

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertTrue(firstWon.get() ^ secondWon.get());
        WebSocketSession winner = firstWon.get() ? first : second;
        WebSocketSession loser = firstWon.get() ? second : first;
        assertSame(winner, registry.current(CLIENT_ID));
        assertFalse(registry.unbind(loser));
        assertTrue(registry.unbind(winner));
    }

    @Test
    void eachClientUsesAStableCoordinationLock() {
        ReentrantLock firstLookup = registry.lockFor(CLIENT_ID);
        ReentrantLock secondLookup = registry.lockFor(CLIENT_ID);

        assertSame(firstLookup, secondLookup);
    }

    @Test
    void invalidArgumentsFailFastAndSocketWithoutIdentityCannotUnbind() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.bind(" ", session(CLIENT_ID)));
        assertThrows(IllegalArgumentException.class,
                () -> registry.bind(CLIENT_ID, null));
        assertThrows(IllegalArgumentException.class,
                () -> registry.current(null));
        assertFalse(registry.unbind(null));
        assertFalse(registry.unbind(session(null)));
    }

    private WebSocketSession session(String clientId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        if (clientId != null) {
            attributes.put(ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);
        }
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}

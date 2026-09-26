package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionContainerTest {

    @Test
    void capacityDefaultsAndValidationAreExplicit() {
        ConnectionCapacityProperties properties = new ConnectionCapacityProperties();

        assertEquals(2000, properties.getNode());
        assertEquals(1000, properties.getRoom());
        assertEquals(100, properties.getIp());
        assertThrows(IllegalArgumentException.class, () -> properties.setNode(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setRoom(-1));
        assertThrows(IllegalArgumentException.class, () -> properties.setIp(0));
    }

    @Test
    void concurrentFirstAccessUsesOneHouseMapAndLosesNoSession() throws Exception {
        JusicProperties.SessionContainer container = new JusicProperties.SessionContainer();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        Set<Object> maps = Collections.synchronizedSet(
                Collections.newSetFromMap(new IdentityHashMap<>())
        );
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 100; i++) {
                final WebSocketSession session = session("session-" + i);
                futures.add(executor.submit(() -> {
                    await(start);
                    maps.add(container.getHouseSession("room-a"));
                    container.getHouseSession("room-a").put(session.getId(), session);
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, maps.size());
        assertEquals(100, container.getHouseSession("room-a").size());
    }

    @Test
    void readOnlyLookupsAndStaleRemovalDoNotRecreateRemovedRoomMaps() {
        JusicProperties.SessionContainer container = new JusicProperties.SessionContainer();

        assertEquals(0, container.getHouseSessionCount("removed-room"));
        assertFalse(container.hasHouseSessions("removed-room"));
        assertNull(container.getSessionIfPresent("removed-room", "missing-session"));
        assertEquals(ConnectionAdmissionResult.Outcome.ALREADY_REMOVED,
                container.remove("missing-session", "removed-room").getOutcome());
        assertFalse(container.get().containsKey("removed-room"));
    }

    @Test
    void repeatedRegistrationIsIdempotentAndSessionIdOwnershipIsProtected() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession first = session("same-id");

        ConnectionAdmissionResult registered = container.register(first, "room-a", "10.0.0.1");
        ConnectionAdmissionResult repeated = container.register(first, "room-a", "10.0.0.1");

        assertEquals(ConnectionAdmissionResult.Outcome.REGISTERED, registered.getOutcome());
        assertEquals(ConnectionAdmissionResult.Outcome.ALREADY_REGISTERED, repeated.getOutcome());
        assertTrue(registered.isChanged());
        assertFalse(repeated.isChanged());
        assertEquals(1, container.getNodeCount());
        assertEquals(1, container.getRoomCount("room-a"));
        assertEquals(1, container.getIpCount("10.0.0.1"));

        ConnectionAdmissionException conflict = assertThrows(
                ConnectionAdmissionException.class,
                () -> container.register(session("same-id"), "room-a", "10.0.0.1")
        );
        assertEquals(ConnectionAdmissionException.Reason.SESSION_ID_CONFLICT, conflict.getReason());
        assertSame(first, container.getHouseSession("room-a").get("same-id"));
        assertEquals(1, container.getNodeCount());
    }

    @Test
    void nodeRoomAndIpLimitsRejectWithoutChangingCounts() {
        JusicProperties.SessionContainer nodeLimited = container(1, 10, 10);
        nodeLimited.register(session("n1"), "a", "ip-1");
        assertReason(ConnectionAdmissionException.Reason.NODE_LIMIT,
                () -> nodeLimited.register(session("n2"), "b", "ip-2"));
        assertEquals(1, nodeLimited.getNodeCount());

        JusicProperties.SessionContainer roomLimited = container(10, 1, 10);
        roomLimited.register(session("r1"), "a", "ip-1");
        assertReason(ConnectionAdmissionException.Reason.ROOM_LIMIT,
                () -> roomLimited.register(session("r2"), "a", "ip-2"));
        assertEquals(1, roomLimited.getRoomCount("a"));

        JusicProperties.SessionContainer ipLimited = container(10, 10, 1);
        ipLimited.register(session("i1"), "a", "ip-1");
        assertReason(ConnectionAdmissionException.Reason.IP_LIMIT,
                () -> ipLimited.register(session("i2"), "b", "ip-1"));
        assertEquals(1, ipLimited.getIpCount("ip-1"));
        assertEquals(1, ipLimited.getNodeCount());
    }

    @Test
    void moveTransfersRoomAndIpCountsWithoutChangingNodeCount() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession session = session("moving");
        container.register(session, "room-a", "ip-a");

        ConnectionAdmissionResult moved = container.move(session, "room-b", "ip-b");
        ConnectionAdmissionResult repeated = container.move(session, "room-b", "ip-b");

        assertEquals(ConnectionAdmissionResult.Outcome.MOVED, moved.getOutcome());
        assertEquals("room-a", moved.getPreviousHouseId());
        assertEquals("room-b", moved.getHouseId());
        assertEquals(ConnectionAdmissionResult.Outcome.ALREADY_REGISTERED, repeated.getOutcome());
        assertFalse(container.get().containsKey("room-a"));
        assertSame(session, container.getHouseSession("room-b").get("moving"));
        assertEquals(1, container.getNodeCount());
        assertEquals(0, container.getRoomCount("room-a"));
        assertEquals(1, container.getRoomCount("room-b"));
        assertEquals(0, container.getIpCount("ip-a"));
        assertEquals(1, container.getIpCount("ip-b"));
    }

    @Test
    void rejectedMoveLeavesSourceMembershipIntact() {
        JusicProperties.SessionContainer container = container(10, 1, 10);
        WebSocketSession moving = session("moving");
        WebSocketSession occupant = session("occupant");
        container.register(moving, "room-a", "ip-a");
        container.register(occupant, "room-b", "ip-b");

        assertReason(ConnectionAdmissionException.Reason.ROOM_LIMIT,
                () -> container.move(moving, "room-b", "ip-a"));

        assertSame(moving, container.getHouseSession("room-a").get("moving"));
        assertFalse(container.getHouseSession("room-b").containsKey("moving"));
        assertEquals(2, container.getNodeCount());
        assertEquals(1, container.getRoomCount("room-a"));
        assertEquals(1, container.getRoomCount("room-b"));
    }

    @Test
    void removalIsIdempotentAndDelayedOldCloseCannotRemoveReplacement() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession oldSession = session("reused-id");
        WebSocketSession replacement = session("reused-id");
        container.register(oldSession, "room-a", "ip-a");

        assertEquals(ConnectionAdmissionResult.Outcome.REMOVED,
                container.remove(oldSession).getOutcome());
        container.register(replacement, "room-a", "ip-a");

        ConnectionAdmissionResult stale = container.remove(oldSession);
        assertEquals(ConnectionAdmissionResult.Outcome.STALE_SESSION, stale.getOutcome());
        assertSame(replacement, container.getHouseSession("room-a").get("reused-id"));
        assertEquals(1, container.getNodeCount());

        assertEquals(ConnectionAdmissionResult.Outcome.REMOVED,
                container.remove(replacement).getOutcome());
        assertEquals(ConnectionAdmissionResult.Outcome.ALREADY_REMOVED,
                container.remove(replacement).getOutcome());
        assertEquals(0, container.getNodeCount());
        assertEquals(0, container.getRoomCount("room-a"));
        assertEquals(0, container.getIpCount("ip-a"));
        assertFalse(container.get().containsKey("room-a"));
    }

    @Test
    void repeatedRoomChurnDoesNotRetainEmptyHouseMaps() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession moving = session("room-churn");
        container.register(moving, "room-0", "ip-a");

        for (int i = 1; i <= 2_000; i++) {
            container.move(moving, "room-" + i, "ip-a");
            assertEquals(1, container.get().size(),
                    "only the session's current house may remain indexed");
        }

        container.remove(moving);

        assertTrue(container.get().isEmpty());
        assertEquals(0, container.getNodeCount());
    }

    @Test
    void identityRemovalSupportsLegacyDirectMapEntriesWithoutTouchingReplacement() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession legacy = session("legacy-id");
        container.getHouseSession("room-a").put(legacy.getId(), legacy);

        assertEquals(ConnectionAdmissionResult.Outcome.REMOVED,
                container.remove(legacy).getOutcome());
        assertFalse(container.get().containsKey("room-a"));
        assertEquals(0, container.getNodeCount());

        WebSocketSession replacement = session("legacy-id");
        container.getHouseSession("room-a").put(replacement.getId(), replacement);
        assertEquals(ConnectionAdmissionResult.Outcome.STALE_SESSION,
                container.remove(legacy).getOutcome());
        assertSame(replacement, container.getHouseSession("room-a").get("legacy-id"));
    }

    @Test
    void removeHouseRejectsActiveSessionsAndCanRemoveAnEmptyHouseRepeatedly() {
        JusicProperties.SessionContainer container = container(10, 10, 10);
        WebSocketSession first = session("a1");
        WebSocketSession second = session("a2");
        WebSocketSession otherRoom = session("b1");
        container.register(first, "room-a", "ip-shared");
        container.register(second, "room-a", "ip-a2");
        container.register(otherRoom, "room-b", "ip-shared");

        assertThrows(IllegalStateException.class,
                () -> container.removeHouse("room-a"));
        assertEquals(3, container.getNodeCount());

        container.remove(first);
        container.remove(second);

        container.removeHouse("room-a");
        container.removeHouse("room-a");

        assertFalse(container.get().containsKey("room-a"));
        assertEquals(1, container.getNodeCount());
        assertEquals(0, container.getRoomCount("room-a"));
        assertEquals(1, container.getRoomCount("room-b"));
        assertEquals(1, container.getIpCount("ip-shared"));
        assertEquals(0, container.getIpCount("ip-a2"));
    }

    @Test
    void concurrentRegistrationsCannotOvershootRoomLimit() throws Exception {
        int roomLimit = 40;
        JusicProperties.SessionContainer container = container(200, roomLimit, 200);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 100; i++) {
                final WebSocketSession session = session("concurrent-" + i);
                final String ip = "ip-" + i;
                futures.add(executor.submit(() -> {
                    await(start);
                    try {
                        container.register(session, "limited-room", ip);
                        admitted.incrementAndGet();
                    } catch (ConnectionAdmissionException exception) {
                        assertEquals(ConnectionAdmissionException.Reason.ROOM_LIMIT, exception.getReason());
                    }
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(roomLimit, admitted.get());
        assertEquals(roomLimit, container.getNodeCount());
        assertEquals(roomLimit, container.getRoomCount("limited-room"));
        assertEquals(roomLimit, container.getHouseSession("limited-room").size());
    }

    private JusicProperties.SessionContainer container(int node, int room, int ip) {
        ConnectionCapacityProperties properties = new ConnectionCapacityProperties();
        properties.setNode(node);
        properties.setRoom(room);
        properties.setIp(ip);
        return new JusicProperties.SessionContainer(properties);
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    private void assertReason(ConnectionAdmissionException.Reason reason, Runnable action) {
        ConnectionAdmissionException exception = assertThrows(ConnectionAdmissionException.class, action::run);
        assertEquals(reason, exception.getReason());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}

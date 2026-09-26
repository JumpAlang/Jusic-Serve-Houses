package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.ConnectionAdmissionResult;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.configuration.SessionCommandLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HouseControllerRoomSwitchQueueCleanupTest {

    private static final String SESSION_ID = "session-switching";
    private static final String REMAINING_SESSION_ID = "session-remaining";
    private static final String SOURCE_HOUSE_ID = "source-house";
    private static final String TARGET_HOUSE_ID = "target-house";
    private static final String CLIENT_IP = "192.0.2.41";

    @Test
    void successfulOrdinaryUserSwitchRemovesOldQueuedMusicAndBroadcastsPickShape() {
        Fixture fixture = successfulFixture("default");
        LinkedList<Music> updatedPickList = new LinkedList<>();
        Music playing = new Music();
        playing.setId("currently-playing");
        Music remaining = new Music();
        remaining.setId("another-users-pick");
        updatedPickList.add(playing);
        updatedPickList.add(remaining);
        when(fixture.musicService.removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID)).thenReturn(2L);
        when(fixture.musicService.getPickList(SOURCE_HOUSE_ID))
                .thenReturn(updatedPickList);

        fixture.controller.enterHouse(request(TARGET_HOUSE_ID),
                accessor(fixture.attributes));

        verify(fixture.musicService).removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID);
        verify(fixture.musicPlayingRepository, never()).getPlaying(SOURCE_HOUSE_ID);
        assertEquals(TARGET_HOUSE_ID, fixture.attributes.get("houseId"));
        assertEquals(1, fixture.properties.getSessionCount(SOURCE_HOUSE_ID));
        assertEquals(1, fixture.properties.getSessionCount(TARGET_HOUSE_ID));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(fixture.sessionService).send(eq(MessageType.PICK),
                payload.capture(), eq(SOURCE_HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertSame(updatedPickList, response.getData());
    }

    @Test
    void successfulOrdinaryUserRoomCreationAlsoCleansOldRoomQueue() {
        CreationFixture fixture = creationFixture("default");
        LinkedList<Music> updatedPickList = new LinkedList<>();
        when(fixture.musicService.removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID)).thenReturn(1L);
        when(fixture.musicService.getPickList(SOURCE_HOUSE_ID))
                .thenReturn(updatedPickList);

        fixture.controller.addHouse(fixture.newHouse,
                accessor(fixture.attributes));

        verify(fixture.musicService).removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID);
        assertEquals(SESSION_ID, fixture.attributes.get("houseId"));
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(fixture.sessionService).send(eq(MessageType.PICK),
                payload.capture(), eq(SOURCE_HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertSame(updatedPickList, response.getData());
    }

    @Test
    void failedRoomCreationMoveDoesNotCleanOldRoomQueue() {
        CreationFixture fixture = creationFixture("default");
        doThrow(new IllegalStateException("redis registration unavailable"))
                .when(fixture.sessionService)
                .putSession(fixture.session, SESSION_ID);

        fixture.controller.addHouse(fixture.newHouse,
                accessor(fixture.attributes));

        verify(fixture.musicService, never()).removeQueuedMusicBySession(
                any(String.class), any(String.class));
        assertEquals(SOURCE_HOUSE_ID, fixture.attributes.get("houseId"));
        verify(fixture.houseContainer).destroy(SESSION_ID);
    }

    @Test
    void roomCreationCleanupFailureKeepsNewRoomAndReportsCreationSuccess() {
        CreationFixture fixture = creationFixture("default");
        doThrow(new IllegalStateException("redis cleanup unavailable"))
                .when(fixture.musicService)
                .removeQueuedMusicBySession(SESSION_ID, SOURCE_HOUSE_ID);

        fixture.controller.addHouse(fixture.newHouse,
                accessor(fixture.attributes));

        assertEquals(SESSION_ID, fixture.attributes.get("houseId"));
        verify(fixture.houseContainer, never()).destroy(SESSION_ID);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(fixture.sessionService).send(eq(fixture.session),
                eq(MessageType.ADD_HOUSE), payload.capture());
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertEquals(SESSION_ID, response.getData());
    }

    @Test
    void privilegedUserRoomCreationKeepsOldRoomQueue() {
        CreationFixture fixture = creationFixture("admin,voter");

        fixture.controller.addHouse(fixture.newHouse,
                accessor(fixture.attributes));

        verify(fixture.musicService, never()).removeQueuedMusicBySession(
                any(String.class), any(String.class));
        assertEquals(SESSION_ID, fixture.attributes.get("houseId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "root", "admin", "root,voter", "admin,voter", "default,picker"
    })
    void privilegedOldRoomRoleKeepsQueuedMusic(String role) {
        Fixture fixture = successfulFixture(role);

        fixture.controller.enterHouse(request(TARGET_HOUSE_ID),
                accessor(fixture.attributes));

        verify(fixture.musicService, never()).removeQueuedMusicBySession(
                any(String.class), any(String.class));
        verify(fixture.musicService, never()).getPickList(SOURCE_HOUSE_ID);
        assertEquals(TARGET_HOUSE_ID, fixture.attributes.get("houseId"));
    }

    @Test
    void noRemovedQueueEntryDoesNotBroadcastRedundantOldRoomPick() {
        Fixture fixture = successfulFixture("default");
        when(fixture.musicService.removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID)).thenReturn(0L);

        fixture.controller.enterHouse(request(TARGET_HOUSE_ID),
                accessor(fixture.attributes));

        verify(fixture.musicService).removeQueuedMusicBySession(
                SESSION_ID, SOURCE_HOUSE_ID);
        verify(fixture.musicService, never()).getPickList(SOURCE_HOUSE_ID);
        verify(fixture.sessionService, never()).send(eq(MessageType.PICK),
                any(), eq(SOURCE_HOUSE_ID));
    }

    @Test
    void cleanupFailureDoesNotUndoSuccessfulRoomSwitch() {
        Fixture fixture = successfulFixture("default");
        doThrow(new IllegalStateException("redis cleanup unavailable"))
                .when(fixture.musicService)
                .removeQueuedMusicBySession(SESSION_ID, SOURCE_HOUSE_ID);

        fixture.controller.enterHouse(request(TARGET_HOUSE_ID),
                accessor(fixture.attributes));

        assertEquals(TARGET_HOUSE_ID, fixture.attributes.get("houseId"));
        verify(fixture.sessionService).putSession(
                fixture.session, TARGET_HOUSE_ID);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(fixture.sessionService).send(eq(fixture.session),
                eq(MessageType.ENTER_HOUSE), payload.capture());
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertEquals(TARGET_HOUSE_ID, response.getData());
    }

    @Test
    void idleTargetRoomStillReplacesQueueAndGoodModeSnapshots() {
        Fixture fixture = successfulFixture("default");
        LinkedList<Music> targetQueue = new LinkedList<>();
        when(fixture.musicPlayingRepository.getPlaying(TARGET_HOUSE_ID))
                .thenReturn(null);
        when(fixture.musicService.getPickList(TARGET_HOUSE_ID))
                .thenReturn(targetQueue);

        fixture.controller.enterHouse(request(TARGET_HOUSE_ID),
                accessor(fixture.attributes));

        verify(fixture.sessionService).send(eq(fixture.session),
                eq(MessageType.PICK), any());
        verify(fixture.sessionService).send(eq(fixture.session),
                eq(MessageType.GOODMODEL), any());
        verify(fixture.sessionService).send(eq(fixture.session),
                eq(MessageType.ROOM_STATE), any());
        verify(fixture.musicService).getPickList(TARGET_HOUSE_ID);
    }

    @Test
    void invalidTargetPasswordDoesNotRemoveQueuedMusic() {
        Fixture fixture = fixture("default", true, "correct-password");
        House request = request(TARGET_HOUSE_ID);
        request.setPassword("wrong-password");

        fixture.controller.enterHouse(request, accessor(fixture.attributes));

        verify(fixture.sessionService, never()).putSession(
                any(WebSocketSession.class), any(String.class));
        verify(fixture.musicService, never()).removeQueuedMusicBySession(
                any(String.class), any(String.class));
        assertEquals(SOURCE_HOUSE_ID, fixture.attributes.get("houseId"));
    }

    @Test
    void enteringCurrentRoomDoesNotRemoveQueuedMusic() {
        SessionService sessionService = mock(SessionService.class);
        MusicService musicService = mock(MusicService.class);
        HouseController controller = new HouseController();
        ReflectionTestUtils.setField(controller, "sessionService", sessionService);
        ReflectionTestUtils.setField(controller, "musicService", musicService);
        ReflectionTestUtils.setField(controller, "sessionCommandLocks",
                new SessionCommandLocks());
        Map<String, Object> attributes = attributes(SOURCE_HOUSE_ID);

        controller.enterHouse(request(SOURCE_HOUSE_ID), accessor(attributes));

        verify(musicService, never()).removeQueuedMusicBySession(
                any(String.class), any(String.class));
        verify(sessionService, never()).getRole(any(String.class), any(String.class));
    }

    private static Fixture successfulFixture(String role) {
        return fixture(role, false, null);
    }

    private static CreationFixture creationFixture(String role) {
        JusicProperties properties = new JusicProperties();
        Map<String, Object> attributes = attributes(SOURCE_HOUSE_ID);
        WebSocketSession session = session(SESSION_ID, attributes);
        WebSocketSession remainingSession = session(REMAINING_SESSION_ID,
                attributes(SOURCE_HOUSE_ID));
        properties.getSession().register(session, SOURCE_HOUSE_ID, CLIENT_IP);
        properties.getSession().register(
                remainingSession, SOURCE_HOUSE_ID, "192.0.2.42");

        SessionService sessionService = mock(SessionService.class);
        when(sessionService.getRole(SESSION_ID, SOURCE_HOUSE_ID))
                .thenReturn(role);
        when(sessionService.putSession(session, SESSION_ID)).thenAnswer(invocation -> {
            properties.getSession().move(session, SESSION_ID, CLIENT_IP);
            attributes.put("houseId", SESSION_ID);
            return User.builder()
                    .sessionId(SESSION_ID)
                    .houseId(SESSION_ID)
                    .role("admin")
                    .build();
        });
        MusicService musicService = mock(MusicService.class);
        HouseContainer houseContainer = mock(HouseContainer.class);
        HouseController controller = new HouseController();
        ReflectionTestUtils.setField(controller, "sessionService", sessionService);
        ReflectionTestUtils.setField(controller, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(controller, "jusicProperties", properties);
        ReflectionTestUtils.setField(controller, "musicService", musicService);
        ReflectionTestUtils.setField(controller, "houseOperationLocks",
                new HouseOperationLocks());
        ReflectionTestUtils.setField(controller, "roomMusicLocks",
                new RoomMusicLocks());
        ReflectionTestUtils.setField(controller, "sessionCommandLocks",
                new SessionCommandLocks());
        ReflectionTestUtils.setField(controller, "configService", mock(ConfigService.class));
        House newHouse = new House();
        newHouse.setName("new room");
        return new CreationFixture(controller, sessionService, musicService,
                houseContainer, session, attributes, newHouse);
    }

    private static Fixture fixture(String role, boolean needPassword,
                                   String password) {
        JusicProperties properties = new JusicProperties();
        Map<String, Object> attributes = attributes(SOURCE_HOUSE_ID);
        WebSocketSession session = session(SESSION_ID, attributes);
        WebSocketSession remainingSession = session(REMAINING_SESSION_ID,
                attributes(SOURCE_HOUSE_ID));
        assertEquals(ConnectionAdmissionResult.Outcome.REGISTERED,
                properties.getSession().register(
                        session, SOURCE_HOUSE_ID, CLIENT_IP).getOutcome());
        assertEquals(ConnectionAdmissionResult.Outcome.REGISTERED,
                properties.getSession().register(
                        remainingSession, SOURCE_HOUSE_ID, "192.0.2.42").getOutcome());

        SessionService sessionService = mock(SessionService.class);
        when(sessionService.getRole(SESSION_ID, SOURCE_HOUSE_ID)).thenReturn(role);
        when(sessionService.putSession(session, TARGET_HOUSE_ID)).thenAnswer(invocation -> {
            properties.getSession().move(session, TARGET_HOUSE_ID, CLIENT_IP);
            attributes.put("houseId", TARGET_HOUSE_ID);
            return User.builder()
                    .sessionId(SESSION_ID)
                    .houseId(TARGET_HOUSE_ID)
                    .role("default")
                    .build();
        });

        House target = request(TARGET_HOUSE_ID);
        target.setNeedPwd(needPassword);
        target.setPassword(password);
        HouseContainer houseContainer = mock(HouseContainer.class);
        when(houseContainer.get(TARGET_HOUSE_ID)).thenReturn(target);

        MusicService musicService = mock(MusicService.class);
        MusicPlayingRepository musicPlayingRepository =
                mock(MusicPlayingRepository.class);
        HouseController controller = new HouseController();
        ReflectionTestUtils.setField(controller, "sessionService", sessionService);
        ReflectionTestUtils.setField(controller, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(controller, "jusicProperties", properties);
        ReflectionTestUtils.setField(controller, "musicService", musicService);
        ReflectionTestUtils.setField(controller, "musicPlayingRepository",
                musicPlayingRepository);
        ReflectionTestUtils.setField(controller, "configService",
                mock(ConfigService.class));
        ReflectionTestUtils.setField(controller, "houseOperationLocks",
                new HouseOperationLocks());
        ReflectionTestUtils.setField(controller, "roomMusicLocks",
                new RoomMusicLocks());
        ReflectionTestUtils.setField(controller, "sessionCommandLocks",
                new SessionCommandLocks());
        return new Fixture(controller, sessionService, musicService,
                musicPlayingRepository, properties, session, attributes);
    }

    private static WebSocketSession session(String sessionId,
                                            Map<String, Object> attributes) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static House request(String houseId) {
        House request = new House();
        request.setId(houseId);
        return request;
    }

    private static Map<String, Object> attributes(String houseId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", houseId);
        attributes.put("remoteAddress", CLIENT_IP);
        return attributes;
    }

    private static StompHeaderAccessor accessor(Map<String, Object> attributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId(SESSION_ID);
        accessor.setSessionAttributes(attributes);
        return accessor;
    }

    private static final class Fixture {
        private final HouseController controller;
        private final SessionService sessionService;
        private final MusicService musicService;
        private final MusicPlayingRepository musicPlayingRepository;
        private final JusicProperties properties;
        private final WebSocketSession session;
        private final Map<String, Object> attributes;

        private Fixture(HouseController controller,
                        SessionService sessionService,
                        MusicService musicService,
                        MusicPlayingRepository musicPlayingRepository,
                        JusicProperties properties,
                        WebSocketSession session,
                        Map<String, Object> attributes) {
            this.controller = controller;
            this.sessionService = sessionService;
            this.musicService = musicService;
            this.musicPlayingRepository = musicPlayingRepository;
            this.properties = properties;
            this.session = session;
            this.attributes = attributes;
        }
    }

    private static final class CreationFixture {
        private final HouseController controller;
        private final SessionService sessionService;
        private final MusicService musicService;
        private final HouseContainer houseContainer;
        private final WebSocketSession session;
        private final Map<String, Object> attributes;
        private final House newHouse;

        private CreationFixture(HouseController controller,
                                SessionService sessionService,
                                MusicService musicService,
                                HouseContainer houseContainer,
                                WebSocketSession session,
                                Map<String, Object> attributes,
                                House newHouse) {
            this.controller = controller;
            this.sessionService = sessionService;
            this.musicService = musicService;
            this.houseContainer = houseContainer;
            this.session = session;
            this.attributes = attributes;
            this.newHouse = newHouse;
        }
    }
}

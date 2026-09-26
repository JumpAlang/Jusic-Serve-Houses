package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.RoomState;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RoomStateSyncTest {
    @Test
    void settingsSnapshotKeepsIndependentModesAndRoomIdentity() {
        ConfigService config = mock(ConfigService.class);
        when(config.getVoteRate("room")).thenReturn(0.35f);
        when(config.getGoodModel("room")).thenReturn(true);
        when(config.getRandomModel("room")).thenReturn(true);
        when(config.getListCircleModel("room")).thenReturn(true);
        when(config.getEnableSearch("room")).thenReturn(false);
        when(config.getEnableSwitch("room")).thenReturn(true);
        RoomState state = RoomState.read(config, "room");
        assertEquals("room", state.getHouseId());
        assertEquals(0.35f, state.getVoteRate());
        assertTrue(state.isGoodModel());
        assertTrue(state.isRandomModel());
        assertTrue(state.isListCircleModel());
        assertFalse(state.isMusicCircleModel());
        assertFalse(state.isEnableSearch());
        assertTrue(state.isEnableSwitch());
    }

    @Test
    void successfulVoteRateChangeBroadcastsPersistedStateToRoom() {
        ConfigService config = mock(ConfigService.class);
        SessionService sessions = mock(SessionService.class);
        MusicController controller = new MusicController();
        ReflectionTestUtils.setField(controller, "configService", config);
        ReflectionTestUtils.setField(controller, "sessionService", sessions);
        when(sessions.getRole("session", "room")).thenReturn("admin");
        when(config.getVoteRate("room")).thenReturn(0.45f);
        StompHeaderAccessor accessor = accessor();
        controller.voteRate(0.45f, accessor);
        verify(config).setVoteRate(0.45f, "room");
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(sessions).send(eq(MessageType.ROOM_STATE), response.capture(), eq("room"));
        assertEquals(0.45f, ((RoomState) response.getValue().getData()).getVoteRate());
    }

    @Test
    void deniedModeChangeDoesNotPublishOrMutateSettings() {
        ConfigService config = mock(ConfigService.class);
        SessionService sessions = mock(SessionService.class);
        MusicController controller = new MusicController();
        ReflectionTestUtils.setField(controller, "configService", config);
        ReflectionTestUtils.setField(controller, "sessionService", sessions);
        when(sessions.getRole("session", "room")).thenReturn("default");
        controller.randomModel(true, accessor());
        controller.banchoose(true, accessor());
        controller.banswitch(true, accessor());
        verifyNoInteractions(config);
        verify(sessions, never()).send(eq(MessageType.ROOM_STATE), any(), anyString());
    }

    @Test
    void absentPermissionSettingsMatchLegacyAllowedDefault() {
        ConfigService config = mock(ConfigService.class);
        when(config.getEnableSearch("room")).thenReturn(null);
        when(config.getEnableSwitch("room")).thenReturn(null);
        RoomState state = RoomState.read(config, "room");
        assertTrue(state.isEnableSearch());
        assertTrue(state.isEnableSwitch());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void permissionChangesBroadcastPersistedAllowFlags(boolean ban) {
        ConfigService config = mock(ConfigService.class);
        SessionService sessions = mock(SessionService.class);
        MusicController controller = new MusicController();
        ReflectionTestUtils.setField(controller, "configService", config);
        ReflectionTestUtils.setField(controller, "sessionService", sessions);
        when(sessions.getRole("session", "room")).thenReturn("admin");
        java.util.concurrent.atomic.AtomicBoolean allowPick = new java.util.concurrent.atomic.AtomicBoolean(ban);
        java.util.concurrent.atomic.AtomicBoolean allowSkip = new java.util.concurrent.atomic.AtomicBoolean(ban);
        doAnswer(call -> { allowPick.set(call.getArgument(0)); return null; })
                .when(config).setEnableSearch(anyBoolean(), eq("room"));
        doAnswer(call -> { allowSkip.set(call.getArgument(0)); return null; })
                .when(config).setEnableSwitch(anyBoolean(), eq("room"));
        when(config.getEnableSearch("room")).thenAnswer(call -> allowPick.get());
        when(config.getEnableSwitch("room")).thenAnswer(call -> allowSkip.get());
        controller.banchoose(ban, accessor());
        controller.banswitch(ban, accessor());
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(sessions, times(2)).send(eq(MessageType.ROOM_STATE), response.capture(), eq("room"));
        RoomState afterPick = (RoomState) response.getAllValues().get(0).getData();
        RoomState afterSkip = (RoomState) response.getAllValues().get(1).getData();
        assertEquals(!ban, afterPick.isEnableSearch());
        assertEquals(ban, afterPick.isEnableSwitch());
        assertEquals(!ban, afterSkip.isEnableSearch());
        assertEquals(!ban, afterSkip.isEnableSwitch());
    }

    private StompHeaderAccessor accessor() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session");
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", "room");
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}

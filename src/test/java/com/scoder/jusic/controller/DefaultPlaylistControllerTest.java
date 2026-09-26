package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.DefaultPlaylistSong;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.security.AbuseAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DefaultPlaylistControllerTest {
    @Test void readsOnlySessionRoomAndReturnsAllPagesWithLegacyFallback() {
        MusicDefaultRepository repository = mock(MusicDefaultRepository.class);
        SessionService sessions = mock(SessionService.class);
        when(repository.size("room")).thenReturn(65L);
        when(repository.page("room",30,30)).thenReturn(Collections.singletonList(DefaultPlaylistSong.legacy("abc___qq")));
        DefaultPlaylistController.Request request = new DefaultPlaylistController.Request();
        request.setPageIndex(2); request.setRequestId("request-2");
        new DefaultPlaylistController(repository,sessions).list(request, accessor());
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(sessions).send(eq("s"),eq(MessageType.DEFAULT_PLAYLIST),response.capture(),eq("room"));
        Map data = (Map)response.getValue().getData();
        assertEquals("room",data.get("scope"));
        assertEquals(65L,data.get("total"));
        assertEquals("request-2",data.get("requestId"));
        assertEquals("qq",((DefaultPlaylistSong)((List)data.get("songs")).get(0)).getSource());
        verify(repository,never()).size("");
    }
    @Test void emptyRoomNeverReadsSystemPlaylistAndInvalidPagesNeverTouchRedis() {
        MusicDefaultRepository repository = mock(MusicDefaultRepository.class);
        SessionService sessions = mock(SessionService.class);
        when(repository.size("room")).thenReturn(0L);
        when(repository.size("")).thenReturn(10L);
        when(repository.page("room",0,30)).thenReturn(Collections.emptyList());
        DefaultPlaylistController controller = new DefaultPlaylistController(repository,sessions);
        controller.list(new DefaultPlaylistController.Request(), accessor());
        verify(repository).page("room",0,30);
        verify(repository,never()).size("");
        verify(repository,never()).page(eq(""),anyLong(),anyInt());
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(sessions).send(eq("s"),eq(MessageType.DEFAULT_PLAYLIST),response.capture(),eq("room"));
        Map data = (Map) response.getValue().getData();
        assertEquals("room",data.get("scope"));
        assertEquals(0L,data.get("total"));
        assertEquals(Collections.emptyList(),data.get("songs"));
        clearInvocations(repository);
        DefaultPlaylistController.Request invalid = new DefaultPlaylistController.Request(); invalid.setPageSize(100000);
        controller.list(invalid,accessor());
        verifyNoInteractions(repository);
        assertTrue(AbuseAction.isKnownDestination("/music/defaultPlaylist"));
        assertEquals(AbuseAction.HOUSE_QUERY,AbuseAction.fromDestination("/music/defaultPlaylist"));
    }
    private StompHeaderAccessor accessor() {
        StompHeaderAccessor a=StompHeaderAccessor.create(StompCommand.SEND);
        a.setSessionId("s"); a.setSessionAttributes(new HashMap<>(Collections.singletonMap("houseId","room"))); return a;
    }
}

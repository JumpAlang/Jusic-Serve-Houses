package com.scoder.jusic.service.imp;

import com.scoder.jusic.repository.MusicPickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicServiceImplSessionCleanupTest {

    private static final String SESSION_ID = "session-owner";
    private static final String HOUSE_ID = "room-a";

    private MusicPickRepository musicPickRepository;
    private MusicServiceImpl service;

    @BeforeEach
    void setUp() {
        musicPickRepository = mock(MusicPickRepository.class);
        service = new MusicServiceImpl(mock(RestTemplate.class));
        ReflectionTestUtils.setField(service,
                "musicPickRepository", musicPickRepository);
    }

    @Test
    void delegatesToPendingQueueRepositoryAndReturnsItsActualCount() {
        when(musicPickRepository.removeQueuedMusicBySession(
                SESSION_ID, HOUSE_ID)).thenReturn(3L);

        assertEquals(3L,
                service.removeQueuedMusicBySession(SESSION_ID, HOUSE_ID));

        verify(musicPickRepository)
                .removeQueuedMusicBySession(SESSION_ID, HOUSE_ID);
    }

    @Test
    void blankIdentifiersReturnZeroWithoutRepositoryAccess() {
        assertEquals(0L, service.removeQueuedMusicBySession(null, HOUSE_ID));
        assertEquals(0L, service.removeQueuedMusicBySession(" ", HOUSE_ID));
        assertEquals(0L, service.removeQueuedMusicBySession(SESSION_ID, null));
        assertEquals(0L, service.removeQueuedMusicBySession(SESSION_ID, " "));

        verify(musicPickRepository, never())
                .removeQueuedMusicBySession(SESSION_ID, HOUSE_ID);
    }
}

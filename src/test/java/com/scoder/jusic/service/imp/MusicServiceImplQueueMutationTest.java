package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPickRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MusicServiceImplQueueMutationTest {

    private static final String HOUSE_ID = "house-a";

    private MusicPickRepository pickRepository;
    private MusicPlayingRepository playingRepository;
    private HouseOperationLocks houseLocks;
    private RoomMusicLocks musicLocks;
    private MusicServiceImpl service;

    @BeforeEach
    void setUp() {
        pickRepository = mock(MusicPickRepository.class);
        playingRepository = mock(MusicPlayingRepository.class);
        houseLocks = new HouseOperationLocks();
        musicLocks = new RoomMusicLocks();
        service = new MusicServiceImpl(mock(RestTemplate.class));
        ReflectionTestUtils.setField(service, "musicPickRepository", pickRepository);
        ReflectionTestUtils.setField(service, "musicPlayingRepository", playingRepository);
        ReflectionTestUtils.setField(service, "houseOperationLocks", houseLocks);
        ReflectionTestUtils.setField(service, "roomMusicLocks", musicLocks);
    }

    @Test
    void deleteUsesOneExactRemoveWhileBothRoomLocksAreHeld() {
        Music queued = music("song-a", "owner");
        Music requested = music("song-a", "owner");
        when(pickRepository.getPickMusicList(HOUSE_ID))
                .thenReturn(Collections.singletonList(queued));
        when(pickRepository.removeOne(queued, HOUSE_ID)).thenAnswer(invocation -> {
            assertRoomLocksHeld();
            return 1L;
        });

        assertTrue(service.deletePickMusic(requested, HOUSE_ID));

        verify(pickRepository).removeOne(queued, HOUSE_ID);
        verifyNoLegacyRewrite();
    }

    @Test
    void topUsesTheSingleKeyAtomicMoveWithoutRewritingTheQueue() {
        Music queued = music("song-a", "owner");
        Music requested = music("song-a", null);
        when(pickRepository.getPickMusicList(HOUSE_ID))
                .thenReturn(Collections.singletonList(queued));
        when(pickRepository.moveExactToTail(eq(queued), anyLong(), eq(HOUSE_ID)))
                .thenAnswer(invocation -> {
                    assertRoomLocksHeld();
                    return 1L;
                });

        assertTrue(service.topPickMusic(requested, HOUSE_ID));

        verify(pickRepository).moveExactToTail(eq(queued), anyLong(), eq(HOUSE_ID));
        verifyNoLegacyRewrite();
    }

    @Test
    void topReturnsFalseWhenTheAtomicMoveLosesAPlaybackRace() {
        Music queued = music("song-a", "owner");
        Music requested = music("song-a", null);
        when(pickRepository.getPickMusicList(HOUSE_ID))
                .thenReturn(Collections.singletonList(queued));
        when(pickRepository.moveExactToTail(eq(queued), anyLong(), eq(HOUSE_ID)))
                .thenReturn(0L);

        assertFalse(service.topPickMusic(requested, HOUSE_ID));

        verifyNoLegacyRewrite();
    }

    @Test
    void clearDeletesTheListKeyInsteadOfMaterializingOrTrimmingIt() {
        when(pickRepository.destroy(HOUSE_ID)).thenAnswer(invocation -> {
            assertRoomLocksHeld();
            return true;
        });

        assertTrue(service.clearPlayList(HOUSE_ID));

        verify(pickRepository).destroy(HOUSE_ID);
        verify(pickRepository, never()).getPickMusicList(HOUSE_ID);
        verifyNoLegacyRewrite();
    }

    @Test
    void clearReturnsFalseWhenRedisDidNotDeleteAQueue() {
        when(pickRepository.destroy(HOUSE_ID)).thenReturn(false);

        assertFalse(service.clearPlayList(HOUSE_ID));

        verify(pickRepository).destroy(HOUSE_ID);
        verifyNoLegacyRewrite();
    }

    @Test
    void queueMutationsRejectBlankRoomAndMusicIdentifiersBeforeRepositoryAccess() {
        Music missingId = music(null, "owner");
        Music blankId = music(" ", "owner");
        Music valid = music("song-a", "owner");

        assertFalse(service.deletePickMusic(null, HOUSE_ID));
        assertFalse(service.deletePickMusic(missingId, HOUSE_ID));
        assertFalse(service.deletePickMusic(blankId, HOUSE_ID));
        assertFalse(service.deletePickMusic(valid, null));
        assertFalse(service.deletePickMusic(valid, " "));

        assertFalse(service.topPickMusic(null, HOUSE_ID));
        assertFalse(service.topPickMusic(missingId, HOUSE_ID));
        assertFalse(service.topPickMusic(blankId, HOUSE_ID));
        assertFalse(service.topPickMusic(valid, null));
        assertFalse(service.topPickMusic(valid, " "));

        assertFalse(service.clearPlayList(null));
        assertFalse(service.clearPlayList(" "));

        verifyNoInteractions(pickRepository);
    }

    @Test
    void anEmptyRoomSnapshotContainsNoNullMusicEntry() {
        when(pickRepository.getPickMusicList(HOUSE_ID))
                .thenReturn(Collections.emptyList());
        when(playingRepository.getPlaying(HOUSE_ID)).thenReturn(null);

        LinkedList<Music> snapshot = service.getPickList(HOUSE_ID);

        assertTrue(snapshot.isEmpty());
        assertEquals(Collections.emptyList(), snapshot);
    }

    private void assertRoomLocksHeld() {
        ReentrantLock houseLock = houseLocks.getLock(HOUSE_ID);
        ReentrantLock musicLock = musicLocks.getLock(HOUSE_ID);
        assertTrue(houseLock.isHeldByCurrentThread());
        assertTrue(musicLock.isHeldByCurrentThread());
    }

    private void verifyNoLegacyRewrite() {
        verify(pickRepository, never()).reset(HOUSE_ID);
        verify(pickRepository, never()).rightPushAll(eq(HOUSE_ID), any(Object[].class));
    }

    private Music music(String id, String sessionId) {
        Music music = new Music();
        music.setId(id);
        music.setName(id);
        music.setSessionId(sessionId);
        return music;
    }
}

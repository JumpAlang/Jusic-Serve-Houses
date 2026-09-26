package com.scoder.jusic.service;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.PickMusicResult;
import com.scoder.jusic.service.imp.LegacyLocalRoomCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyLocalRoomCommandServiceTest {

    private static final String SESSION_ID = "session-1";
    private static final String HOUSE_ID = "house-1";

    @Mock
    private HouseOperationLocks houseOperationLocks;
    @Mock
    private RoomMusicLocks roomMusicLocks;
    @Mock
    private HouseContainer houseContainer;
    @Mock
    private JusicProperties jusicProperties;
    @Mock
    private MusicService musicService;
    @Mock
    private ConfigService configService;
    @Mock
    private SessionService sessionService;
    @Mock
    private MusicJob musicJob;

    @InjectMocks
    private LegacyLocalRoomCommandService commandService;

    private ReentrantLock lock;
    private ReentrantLock musicLock;

    @BeforeEach
    void configureLock() {
        lock = new ReentrantLock();
        musicLock = new ReentrantLock();
        when(houseOperationLocks.getLock(HOUSE_ID)).thenReturn(lock);
        when(roomMusicLocks.getLock(HOUSE_ID)).thenReturn(musicLock);
    }

    @Test
    void successfulCommitUsesLatestRoomStateAndReturnsLegacySnapshot() {
        House house = house(false);
        Music music = music();
        LinkedList<Music> snapshot = new LinkedList<>();
        snapshot.add(music);
        when(houseContainer.get(HOUSE_ID)).thenReturn(house);
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(2L);
        when(musicService.toPick(SESSION_ID, music, HOUSE_ID, "qq")).thenReturn(music);
        when(musicService.getPickList(HOUSE_ID)).thenReturn(snapshot);

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music);

        assertTrue(result.isSuccess());
        assertSame(music, result.getMusic());
        assertSame(snapshot, result.getPickList());
        verify(musicJob).wakeHouse(HOUSE_ID);
        assertFalse(lock.isLocked());
        assertFalse(musicLock.isLocked());
    }

    @Test
    void wakeFailureAfterCommitStillReturnsSuccessfulPick() {
        Music music = music();
        LinkedList<Music> snapshot = new LinkedList<>();
        snapshot.add(music);
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(false));
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(0L);
        when(musicService.toPick(SESSION_ID, music, HOUSE_ID, "qq"))
                .thenReturn(music);
        when(musicService.getPickList(HOUSE_ID)).thenReturn(snapshot);
        doThrow(new IllegalStateException("wake failed"))
                .when(musicJob).wakeHouse(HOUSE_ID);

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music);

        assertTrue(result.isSuccess());
        assertSame(music, result.getMusic());
        assertSame(snapshot, result.getPickList());
    }

    @Test
    void snapshotFailureAfterCommitStillReturnsSuccessfulPick() {
        Music music = music();
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(false));
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(0L);
        when(musicService.toPick(SESSION_ID, music, HOUSE_ID, "qq"))
                .thenReturn(music);
        when(musicService.getPickList(HOUSE_ID))
                .thenThrow(new IllegalStateException("snapshot failed"));

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music);

        assertTrue(result.isSuccess());
        assertSame(music, result.getMusic());
        assertNotNull(result.getPickList());
        assertTrue(result.getPickList().isEmpty());
        verify(musicJob).wakeHouse(HOUSE_ID);
    }

    @Test
    void roomDeletionIsRecheckedAfterProviderResolution() {
        when(houseContainer.get(HOUSE_ID)).thenReturn(null);

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music());

        assertFalse(result.isSuccess());
        assertEquals("房间不存在", result.getMessage());
        verify(musicService, never()).toPick(
                SESSION_ID, music(), HOUSE_ID, "qq");
        verify(musicJob, never()).wakeHouse(HOUSE_ID);
    }

    @Test
    void permissionIsRecheckedInsideTheCriticalSection() {
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(false));
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(false);
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("viewer");

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music());

        assertFalse(result.isSuccess());
        assertEquals("当前禁止点歌", result.getMessage());
        verify(musicService, never()).toPick(
                SESSION_ID, music(), HOUSE_ID, "qq");
    }

    @Test
    void permanentRoomUsesLatestForeverLimitAndRejectsAtCapacity() {
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(true));
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getForeverlistSize()).thenReturn(5);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(5L);

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music());

        assertFalse(result.isSuccess());
        assertEquals("最多能点播5首歌", result.getMessage());
        verify(musicService, never()).toPick(
                SESSION_ID, music(), HOUSE_ID, "qq");
    }

    @Test
    void blacklistAndDuplicateChecksRunBeforeTheLegacyWrite() {
        Music music = music();
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(false));
        allowCurrentMembership();
        when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(0L);
        when(musicService.isBlack("song-1", HOUSE_ID)).thenReturn(true);

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music);

        assertFalse(result.isSuccess());
        assertEquals("点歌失败, 音乐已被拉黑", result.getMessage());
        verify(musicService, never()).isPicked("song-1", HOUSE_ID);
        verify(musicService, never()).toPick(SESSION_ID, music, HOUSE_ID, "qq");
    }

    @Test
    void runtimeFailureAlwaysReleasesTheStripeLock() {
        when(houseContainer.get(HOUSE_ID)).thenThrow(new IllegalStateException("boom"));

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music());

        assertFalse(result.isSuccess());
        assertEquals("点歌失败,请稍后重试", result.getMessage());
        assertFalse(lock.isLocked());
        assertFalse(musicLock.isLocked());
    }

    @Test
    void delayedPickAfterRoomSwitchCannotReenterOldQueue() {
        when(houseContainer.get(HOUSE_ID)).thenReturn(house(false));

        PickMusicResult result = commandService.commitPick(
                SESSION_ID, HOUSE_ID, "qq", music());

        assertFalse(result.isSuccess());
        assertEquals("当前禁止点歌", result.getMessage());
        verify(configService, never()).getEnableSearch(HOUSE_ID);
        verify(musicService, never()).toPick(
                SESSION_ID, music(), HOUSE_ID, "qq");
        assertFalse(lock.isLocked());
        assertFalse(musicLock.isLocked());
    }

    private void allowCurrentMembership() {
        when(jusicProperties.getSessionIfPresent(HOUSE_ID, SESSION_ID))
                .thenReturn(mock(WebSocketSession.class));
    }

    private House house(boolean permanent) {
        House house = new House();
        house.setId(HOUSE_ID);
        house.setEnableStatus(permanent);
        return house;
    }

    private Music music() {
        Music music = new Music();
        music.setId("song-1");
        music.setName("Song One");
        music.setSource("qq");
        music.setUrl("https://music.invalid/song-1.mp3");
        return music;
    }
}

package com.scoder.jusic.service;

import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.PickMusicResult;

/**
 * Mutation boundary for room-scoped commands.
 *
 * <p>The current implementation preserves the legacy Redis schema and uses a
 * process-local critical section. A future multi-node adapter can replace it
 * after every reader and writer for the room has migrated to Redis v2.</p>
 */
public interface RoomCommandService {

    PickMusicResult commitPick(String sessionId,
                               String houseId,
                               String source,
                               Music music);
}

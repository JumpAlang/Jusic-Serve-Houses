package com.scoder.jusic.repository;

/**
 * @author H
 */
public interface MusicDefaultRepository {
    Long addSongs(java.util.List<com.scoder.jusic.model.DefaultPlaylistSong> songs, String houseId);
    java.util.List<com.scoder.jusic.model.DefaultPlaylistSong> page(String houseId, long offset, int limit);

    /**
     * destroy
     *
     * @return -
     */
    Boolean destroy(String houseId);

    /**
     * initialize
     *
     * @return -
     */
    Long initialize(String houseId);

    /**
     * size
     *
     * @return -
     */
    Long size(String houseId);

    /**
     * get random member
     *
     * @return random member
     */
    String randomMember(String houseId);

    /**
     * Returns the next member in a room-specific playback cycle. Random mode
     * shuffles once per cycle, so no member repeats until the cycle is empty.
     */
    String nextMember(String playlistHouseId, String playbackHouseId, boolean random);

    void remove(String id,String houseId);

    /**
     * add value
     *
     * @param value ...value
     * @return long
     */
    Long add(String[] value,String houseId);

}

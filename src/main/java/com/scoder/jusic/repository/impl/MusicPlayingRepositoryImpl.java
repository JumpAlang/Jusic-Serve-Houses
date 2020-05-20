package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPlayingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author H
 */
@Repository
public class MusicPlayingRepositoryImpl implements MusicPlayingRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy(String houseId) {
        redisTemplate.opsForList().trim(redisKeys.getPlayingList()+houseId, 1, 0);
    }

    @Override
    public Long leftPush(Music pick,String houseId) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPlayingList()+houseId, pick);
    }

    @Override
    public Music pickToPlaying(String houseId) {
        return (Music) redisTemplate.opsForList()
                .rightPopAndLeftPush(redisKeys.getPickList()+houseId, redisKeys.getPlayingList()+houseId);
    }

    @Override
    public void keepTheOne(String houseId) {
        redisTemplate.opsForList()
                .trim(redisKeys.getPlayingList()+houseId, 0, 0);
    }

    @Override
    public Music getPlaying(String houseId) {
        return (Music) redisTemplate.opsForList()
                .index(redisKeys.getPlayingList()+houseId, 0);
    }

}

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
    public void destroy() {
        redisTemplate.opsForList().trim(redisKeys.getPlayingList(), 1, 0);
    }

    @Override
    public Long leftPush(Music pick) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPlayingList(), pick);
    }

    @Override
    public Music pickToPlaying() {
        return (Music) redisTemplate.opsForList()
                .rightPopAndLeftPush(redisKeys.getPickList(), redisKeys.getPlayingList());
    }

    @Override
    public void keepTheOne() {
        redisTemplate.opsForList()
                .trim(redisKeys.getPlayingList(), 0, 0);
    }

    @Override
    public Music getPlaying() {
        return (Music) redisTemplate.opsForList()
                .index(redisKeys.getPlayingList(), 0);
    }

}

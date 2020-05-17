package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
@Repository
public class MusicPickRepositoryImpl implements MusicPickRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy() {
        redisTemplate.opsForList()
                .trim(redisKeys.getPickList(), 1, 0);
    }

    @Override
    public Long leftPush(Music pick) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPickList(), pick);
    }

    @Override
    public Long leftPushAll(Object... value) {
        return redisTemplate.opsForList()
                .leftPushAll(redisKeys.getPickList(), value);
    }

    @Override
    public Long rightPushAll(Object... value) {
        return redisTemplate.opsForList()
                .rightPushAll(redisKeys.getPickList(), value);
    }

    @Override
    public Long size() {
        return redisTemplate.opsForList()
                .size(redisKeys.getPickList());
    }

    /**
     * clear the pick list.
     */
    @Override
    public void reset() {
        redisTemplate.opsForList()
                .trim(redisKeys.getPickList(), 1, 0);
    }

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    @Override
    public List<Music> getPickMusicList() {
        Long size = this.size();
        size = size == null ? 0 : size;
        return (List<Music>) redisTemplate.opsForList()
                .range(redisKeys.getPickList(), 0, size);
    }

}

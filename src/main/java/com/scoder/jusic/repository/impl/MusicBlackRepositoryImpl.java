package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicBlackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author H
 */
@Repository
@Slf4j
public class MusicBlackRepositoryImpl implements MusicBlackRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean isMember(String id) {
        return redisTemplate.opsForSet()
                .isMember(redisKeys.getBlackSet(), id);
    }

    @Override
    public Long add(String value) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getBlackSet(), value);
    }

    @Override
    public Long remove(String id) {
        return redisTemplate.opsForSet()
                .remove(redisKeys.getBlackSet(), id);
    }

    @Override
    public Set showBlackList() {
        Set blackList = redisTemplate.opsForSet().members(redisKeys.getBlackSet());
        return blackList;
    }
}

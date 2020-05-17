package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicVoteRepository;
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
public class MusicVoteRepositoryImpl implements MusicVoteRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long destroy() {
        Set members = this.members();
        return members != null && members.size() > 0 ? redisTemplate.opsForSet()
                .remove(redisKeys.getSkipSet(), members.toArray()) : 0;
    }

    @Override
    public Long add(Object... value) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getSkipSet(), value);
    }

    @Override
    public Long size() {
        return redisTemplate.opsForSet()
                .size(redisKeys.getSkipSet());
    }

    @Override
    public void reset() {
        Set members = this.members();
        if (null != members && members.size() > 0) {
            redisTemplate.opsForSet()
                    .remove(redisKeys.getSkipSet(), members.toArray());
        }
    }

    @Override
    public Set members() {
        return redisTemplate.opsForSet()
                .members(redisKeys.getSkipSet());
    }

}

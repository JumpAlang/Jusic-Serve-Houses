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
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getBlackSet()+houseId);
    }

    @Override
    public boolean isMember(String id,String houseId) {
        return redisTemplate.opsForSet()
                .isMember(redisKeys.getBlackSet()+houseId, id);
    }

    @Override
    public Long add(String value,String houseId) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getBlackSet()+houseId, value);
    }

    @Override
    public Long remove(String id,String houseId) {
        return redisTemplate.opsForSet()
                .remove(redisKeys.getBlackSet()+houseId, id);
    }

    @Override
    public Set showBlackList(String houseId) {
        Set blackList = redisTemplate.opsForSet().members(redisKeys.getBlackSet()+houseId);
        return blackList;
    }
}

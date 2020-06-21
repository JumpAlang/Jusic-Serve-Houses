package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author H
 */
@Repository
@Slf4j
public class SessionRepositoryImpl implements SessionRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
//        Set keys = redisTemplate.opsForHash()
//                .keys(redisKeys.getSessionHash()+houseId);
//        return keys.size() > 0 ? redisTemplate.opsForHash()
//                .delete(redisKeys.getSessionHash()+houseId, keys.toArray()) : 0;
        return redisTemplate.delete(redisKeys.getSessionHash()+houseId);
    }

    @Override
    public User getSession(String sessionId,String houseId) {
        return (User) redisTemplate.opsForHash()
                .get(redisKeys.getSessionHash()+houseId, sessionId);
    }

    @Override
    public List getSession(String houseId) {
        return redisTemplate.opsForHash()
                .values(redisKeys.getSessionHash()+houseId);
    }

    @Override
    public void setSession(User user,String houseId) {
        redisTemplate.opsForHash()
                .put(redisKeys.getSessionHash()+houseId, user.getSessionId(), user);
    }

    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForHash()
                .size(redisKeys.getSessionHash()+houseId);
    }

    @Override
    public Long removeSession(String sessionId,String houseId) {
        return redisTemplate.opsForHash()
                .delete(redisKeys.getSessionHash()+houseId, sessionId);
    }

}

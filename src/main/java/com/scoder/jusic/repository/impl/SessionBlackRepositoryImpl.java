package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionBlackRepository;
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
public class SessionBlackRepositoryImpl implements SessionBlackRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long destroy() {
        Set keys = redisTemplate.opsForHash()
                .keys(redisKeys.getSessionBlackHash());
        return keys.size() > 0 ? redisTemplate.opsForHash()
                .delete(redisKeys.getSessionBlackHash(), keys.toArray()) : 0;
    }

    @Override
    public User getSession(String sessionId) {
        return (User) redisTemplate.opsForHash()
                .get(redisKeys.getSessionBlackHash(), sessionId);
    }

    @Override
    public void setSession(User user) {
        redisTemplate.opsForHash()
                .put(redisKeys.getSessionBlackHash(), user.getSessionId(), user);
    }

    @Override
    public Long removeSession(String sessionId) {
        return redisTemplate.opsForHash()
                .delete(redisKeys.getSessionBlackHash(), sessionId);
    }

    @Override
    public Set showBlackList() {
        Set keys = redisTemplate.opsForHash()
                .keys(redisKeys.getSessionBlackHash());
        return keys;
    }

}

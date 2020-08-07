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
    public Boolean destroy(String houseId) {
//        Set keys = redisTemplate.opsForHash()
//                .keys(redisKeys.getSessionBlackHash()+houseId);
//        return keys.size() > 0 ? redisTemplate.opsForHash()
//                .delete(redisKeys.getSessionBlackHash()+houseId, keys.toArray()) : 0;
        return redisTemplate.delete(redisKeys.getSessionBlackHash()+houseId);
    }

    @Override
    public User getSession(String sessionId,String ip, String houseId) {
        User user = (User) redisTemplate.opsForHash()
                .get(redisKeys.getSessionBlackHash()+houseId, sessionId);
        if(user == null){
            user = (User) redisTemplate.opsForHash()
                    .get(redisKeys.getSessionBlackHash()+houseId, ip);
        }
        return user;
    }

    @Override
    public void setSession(User user,String houseId) {
        redisTemplate.opsForHash()
                .put(redisKeys.getSessionBlackHash()+houseId, user.getSessionId(), user);
        redisTemplate.opsForHash()
                .put(redisKeys.getSessionBlackHash()+houseId, user.getRemoteAddress(), user);
    }

    @Override
    public Long removeSession(String sessionId,String houseId) {
        User user = (User) redisTemplate.opsForHash()
                .get(redisKeys.getSessionBlackHash()+houseId, sessionId);
        if(user != null){
            redisTemplate.opsForHash()
                    .delete(redisKeys.getSessionBlackHash()+houseId, user.getRemoteAddress());
        }
        return redisTemplate.opsForHash()
                .delete(redisKeys.getSessionBlackHash()+houseId, sessionId);
    }

    @Override
    public Set showBlackList(String houseId) {
        Set keys = redisTemplate.opsForHash()
                .keys(redisKeys.getSessionBlackHash()+houseId);
        return keys;
    }

}

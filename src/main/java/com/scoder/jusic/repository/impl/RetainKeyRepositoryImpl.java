package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.RetainKey;
import com.scoder.jusic.repository.RetainKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JumpAlang
 * @create 2020-07-06 16:27
 */
@Repository
@Slf4j
public class RetainKeyRepositoryImpl implements RetainKeyRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List showRetainKey() {
        List values = redisTemplate.opsForHash()
                .values(redisKeys.getRetainKeyHash());
        return values;
    }

    @Override
    public void addRetainKey(RetainKey retainKey) {
        redisTemplate.opsForHash()
                .put(redisKeys.getRetainKeyHash(), retainKey.getKey(),retainKey);
    }

    @Override
    public void removeRetainKey(String retainKey) {
        redisTemplate.opsForHash()
                .delete(redisKeys.getRetainKeyHash(), retainKey);
    }

    @Override
    public RetainKey getRetainKey(String retainKey) {
        return (RetainKey) redisTemplate.opsForHash()
                .get(redisKeys.getRetainKeyHash(), retainKey);
    }

    @Override
    public void updateRetainKey(RetainKey retainKey) {
        this.addRetainKey(retainKey);
    }
}

package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author H
 */
@Repository
public class MusicDefaultRepositoryImpl implements MusicDefaultRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getDefaultSet()+"_"+houseId);
//        Set members = redisTemplate.opsForSet()
//                .members(redisKeys.getDefaultSet());
//        return members != null && members.size() > 0 ? redisTemplate.opsForSet()
//                .remove(redisKeys.getDefaultSet(), members.toArray()) : 0;
    }

    @Override
    public Long initialize(String houseId) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getDefaultSet()+"_"+houseId, JusicProperties.getDefaultListForRepository().toArray());
    }

    /**
     * set
     *
     * @return long
     */
    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForSet()
                .size(redisKeys.getDefaultSet()+"_"+houseId);
    }

    @Override
    public String randomMember(String houseId) {
        Object obj = redisTemplate.opsForSet()
                .randomMember(redisKeys.getDefaultSet()+"_"+houseId);
        if(obj != null){
            return (String) obj;
        }else{
            return "25718007";
        }

    }

    @Override
    public void remove(String id, String houseId) {
        redisTemplate.opsForSet()
                .remove(redisKeys.getDefaultSet()+"_"+houseId,id);
    }

    @Override
    public Long add(String[] value,String houseId) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getDefaultSet()+"_"+houseId, value);
    }
}

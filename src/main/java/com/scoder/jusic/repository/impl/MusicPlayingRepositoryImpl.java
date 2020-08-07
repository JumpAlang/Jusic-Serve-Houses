package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.util.RandomUtils;
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
    public Boolean destroy(String houseId) {
//        redisTemplate.opsForList().trim(redisKeys.getPlayingList()+houseId, 1, 0);
        return redisTemplate.delete(redisKeys.getPlayingList()+houseId);
    }

    @Override
    public Long leftPush(Music pick,String houseId) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPlayingList()+houseId, pick);
    }

    @Override
    public Music pickToPlaying(String houseId) {
        return (Music) redisTemplate.opsForList()
                .rightPopAndLeftPush(redisKeys.getPickList()+houseId, redisKeys.getPlayingList()+houseId);
    }

    @Override
    public Music randomToPlaying(String houseId) {
        int size = redisTemplate.opsForList()
                .size(redisKeys.getPickList()+houseId).intValue();
        if(size == 1){
            return this.pickToPlaying(houseId);
        }
        int index = RandomUtils.getRandNumber(size);
        Music music =(Music)redisTemplate.opsForList().index(redisKeys.getPickList()+houseId,index);
        this.leftPush(music,houseId);
        long count = redisTemplate.opsForList().remove(redisKeys.getPickList()+houseId,1,music);
        return music;
    }

    @Override
    public void keepTheOne(String houseId) {
        redisTemplate.opsForList()
                .trim(redisKeys.getPlayingList()+houseId, 0, 0);
    }

    @Override
    public Music getPlaying(String houseId) {
        return (Music) redisTemplate.opsForList()
                .index(redisKeys.getPlayingList()+houseId, 0);
    }

}

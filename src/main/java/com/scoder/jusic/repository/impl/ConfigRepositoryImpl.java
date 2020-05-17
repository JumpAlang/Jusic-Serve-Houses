package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author H
 */
@Repository
public class ConfigRepositoryImpl implements ConfigRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long destroy() {
        Set keys = redisTemplate.opsForHash()
                .keys(redisKeys.getConfigHash());
        return keys.size() > 0 ? redisTemplate.opsForHash()
                .delete(redisKeys.getConfigHash(), keys.toArray()) : 0;
    }

    @Override
    public void initialize() {
        this.put(redisKeys.getRedisRoleRoot(), jusicProperties.getRoleRootPassword());
        this.put(redisKeys.getRedisRoleAdmin(), jusicProperties.getRoleAdminPassword());
        this.put(redisKeys.getVoteSkipRate(), jusicProperties.getVoteRate());
    }

    @Override
    public Object get(Object hashKey) {
        return redisTemplate.opsForHash()
                .get(redisKeys.getConfigHash(), hashKey);
    }

    @Override
    public void put(Object hashKey, Object value) {
        redisTemplate.opsForHash()
                .put(redisKeys.getConfigHash(), hashKey, value);
    }

    @Override
    public void putAll(Map<String, Object> map) {
        redisTemplate.opsForHash()
                .putAll(redisKeys.getConfigHash(), map);
    }

    @Override
    public String getPassword(String role) {
        return (String) this.get(role);
    }

    @Override
    public void setPassword(String role, String password) {
        this.put(role, password);
    }

    @Override
    public void initRootPassword() {
        this.setPassword(redisKeys.getRedisRoleRoot(), jusicProperties.getRoleRootPassword());
    }

    @Override
    public void initAdminPassword() {
        this.setPassword(redisKeys.getRedisRoleAdmin(), jusicProperties.getRoleAdminPassword());
    }

    @Override
    public String getRootPassword() {
        return this.getPassword(redisKeys.getRedisRoleRoot());
    }

    @Override
    public String getAdminPassword() {
        return this.getPassword(redisKeys.getRedisRoleAdmin());
    }

    @Override
    public Long getLastMusicDuration() {
        return (Long) this.get(redisKeys.getLastMusicDuration());
    }

    @Override
    public void setLastMusicDuration(Long duration) {
        this.put(redisKeys.getLastMusicDuration(), duration);
    }

    @Override
    public Long getLastMusicPushTime() {
        return (Long) this.get(redisKeys.getLastMusicPushTime());
    }

    @Override
    public void setLastMusicPushTime(Long pushTime) {
        this.put(redisKeys.getLastMusicPushTime(), pushTime);
    }

    @Override
    public void setLastMusicPushTimeAndDuration(Long pushTime, Long duration) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(redisKeys.getLastMusicPushTime(), pushTime);
        map.put(redisKeys.getLastMusicDuration(), duration);
        this.putAll(map);
    }

    @Override
    public Boolean getPushSwitch() {
        return (Boolean) this.get(redisKeys.getSwitchMusicPush());
    }

    @Override
    public void setPushSwitch(boolean pushSwitch) {
        this.put(redisKeys.getSwitchMusicPush(), pushSwitch);
    }

    @Override
    public Float getVoteRate() {
        return (float) this.get(redisKeys.getVoteSkipRate());
    }

    @Override
    public Boolean getEnableSwitch() {
        return (Boolean) this.get(redisKeys.getSwitchMusicEnable());
    }

    @Override
    public void setEnableSwitch(boolean enableSwitch) {
        this.put(redisKeys.getSwitchMusicEnable(), enableSwitch);
    }

    @Override
    public Boolean getEnableSearch() {
        return (Boolean) this.get(redisKeys.getSearchMusicEnable());
    }

    @Override
    public void setEnableSearch(boolean enableSearch) {
        this.put(redisKeys.getSearchMusicEnable(), enableSearch);
    }

    @Override
    public Boolean getGoodModel() {
        return (Boolean) this.get(redisKeys.getGoodModel());
    }

    @Override
    public void setGoodModel(boolean goodModel) {
        this.put(redisKeys.getGoodModel(), goodModel);
    }
}

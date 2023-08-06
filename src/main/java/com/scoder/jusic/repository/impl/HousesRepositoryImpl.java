package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.HousesRespository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author JumpAlang
 * @create 2020-06-21 22:18
 */
@Repository
@Slf4j
public class HousesRepositoryImpl implements HousesRespository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(List houses) {
        this.reset();
        this.rightPushAll(houses.toArray());
        return true;
    }

    @Override
    public CopyOnWriteArrayList<House> initialize() {
        if(this.size() == 0){
            CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<House>();
            houses.add(defaultHouse());
            return houses;
        }else{
            CopyOnWriteArrayList<House> houses = this.get();
            House house = defaultHouse();
            if(houses.contains(house)){
                return houses;
            }else{
                CopyOnWriteArrayList<House> newHouses = new CopyOnWriteArrayList<>();
                newHouses.add(house);
                newHouses.addAll(houses);
                return newHouses;
            }
        }
    }

    private House defaultHouse(){
        House house = new House();
        house.setEnableStatus(true);
        house.setName(JusicProperties.HOUSE_DEFAULT_NAME);
        house.setId(JusicProperties.HOUSE_DEFAULT_ID);
        house.setDesc(JusicProperties.HOUSE_DEFAULT_DESC);
        house.setCreateTime(System.currentTimeMillis());
        house.setNeedPwd(false);
        house.setRemoteAddress("127.0.0.1");
        return house;
    }

    @Override
    public Long add(Object... value) {
        return redisTemplate.opsForList()
                .rightPush(redisKeys.getHouses(), value);
    }


    @Override
    public Long size() {
        return redisTemplate.opsForList()
                .size(redisKeys.getHouses());
    }

    @Override
    public void reset() {
        redisTemplate.opsForList()
                .trim(redisKeys.getHouses(), 1, 0);
    }

    @Override
    public Long rightPushAll(Object... value) {
        return redisTemplate.opsForList()
                .rightPushAll(redisKeys.getHouses(), value);
    }

    @Override
    public CopyOnWriteArrayList<House> get() {
        Long size = this.size();
        size = size == null ? 0 : size;
        CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<>();

        List<House> houseOrigin = (List<House>) redisTemplate.opsForList()
                .range(redisKeys.getHouses(), 0, size);
        for(House house : houseOrigin){
            houses.add(house);
        }
        return houses;
    }

    @Override
    public Set<String> allKeys() {
        Set<String> keys = new HashSet();
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        RedisConnection redisConnection = connectionFactory.getConnection();
        Cursor<byte[]> scan = redisConnection.scan(ScanOptions.scanOptions().match("*").count(33333).build());
        while (scan.hasNext()) {
            //找到一次就添加一次
            keys.add(new String(scan.next()));
        }
        try{
            if (scan != null) {
                scan.close();
            }
        } catch (IOException e) {
            log.error("scan遍历key关闭游标异常", e);
        }
        return keys;
    }

    @Override
    public void delKey(String keyName) {
        redisTemplate.delete(keyName);
    }
}

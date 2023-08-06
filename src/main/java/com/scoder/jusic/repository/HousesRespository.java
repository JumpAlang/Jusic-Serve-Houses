package com.scoder.jusic.repository;

import com.scoder.jusic.model.House;

import java.util.List;
import java.util.Set;

/**
 * @author JumpAlang
 * @create 2020-06-21 22:03
 */
public interface HousesRespository {

    Boolean destroy(List<House> houses);

    List<House> initialize();

    Long rightPushAll(Object... value);

    Long size();


    void reset();

    Long add(Object... value);

    List<House> get();

    Set<String> allKeys();

    void delKey(String keyName);
}

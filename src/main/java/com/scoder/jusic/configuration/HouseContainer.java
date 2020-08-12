package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.model.RetainKey;
import com.scoder.jusic.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author alang
 * @create 2020-05-20 23:28
 */
@Component
@Slf4j
public class HouseContainer {
    private final ConfigRepository configRepository;
    private final SessionRepository sessionRepository;
    private final MusicDefaultRepository musicDefaultRepository;
    private final MusicPlayingRepository musicPlayingRepository;
    private final MusicPickRepository musicPickRepository;
    private final MusicVoteRepository musicVoteRepository;
    private final MusicBlackRepository musicBlackRepository;
    private final SessionBlackRepository sessionBlackRepository;
    private final JusicProperties jusicProperties;
    private final HousesRespository housesRespository;
    private final RetainKeyRepository retainKeyRepository;
    private CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<House>();

    public HouseContainer(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository,MusicBlackRepository musicBlackRepository,SessionBlackRepository sessionBlackRepository,JusicProperties jusicProperties,HousesRespository housesRespository,RetainKeyRepository retainKeyRepository) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.musicDefaultRepository = musicDefaultRepository;
        this.musicPlayingRepository = musicPlayingRepository;
        this.musicPickRepository = musicPickRepository;
        this.musicVoteRepository = musicVoteRepository;
        this.musicBlackRepository = musicBlackRepository;
        this.sessionBlackRepository = sessionBlackRepository;
        this.jusicProperties = jusicProperties;
        this.housesRespository = housesRespository;
        this.retainKeyRepository = retainKeyRepository;
//        House house = new House();
//        house.setName(JusicProperties.HOUSE_DEFAULT_NAME);
//        house.setId(JusicProperties.HOUSE_DEFAULT_ID);
//        house.setDesc(JusicProperties.HOUSE_DEFAULT_DESC);
//        houses.add(house);
    }

    public int size(){
        return houses.size();
    }

    public void setHouses(CopyOnWriteArrayList<House> houses){
        this.houses = houses;
    }

    public boolean isBeyondIpHouse(String ip,int limit){
        int count = 0;
        for(House house : houses){
            if(house.getRemoteAddress().equals(ip)){
                if(++count >= limit){
                    return true;
                }
            }
        }
        return false;
    }

    public CopyOnWriteArrayList<House> getHouses(){
        return this.houses;
    }

    public House get(String id){
        House house = new House();
        house.setId(id);
        int indexOf = houses.indexOf(house);
        if(indexOf != -1){
            return houses.get(indexOf);
        }
        return null;
    }

    public Boolean contains(String id){
        House house = new House();
        house.setId(id);
        return houses.contains(house);
    }

    public void add(House house){
//        try{
//            sessionRepository.destroy(house.getId());
//            configRepository.destroy(house.getId());
//            musicPlayingRepository.destroy(house.getId());
//            musicPickRepository.destroy(house.getId());
//            musicVoteRepository.destroy(house.getId());
//            musicBlackRepository.destroy(house.getId());
//            sessionBlackRepository.destroy(house.getId());
//        }catch (Exception e){
//            log.error("houseId:{},houseName:{},message:[{}]",house.getId(),house.getName(),e.getMessage());
//        }
        configRepository.initialize(house.getId());
        houses.add(house);
        if(house.getEnableStatus() != null && house.getEnableStatus()){
            housesRespository.add(house);
        }
    }

    public void destroy(String id){
        try{
            this.remove(id);
            jusicProperties.removeSessions(id);
            sessionRepository.destroy(id);
            configRepository.destroy(id);
            musicPlayingRepository.destroy(id);
            musicPickRepository.destroy(id);
            musicVoteRepository.destroy(id);
            musicBlackRepository.destroy(id);
            sessionBlackRepository.destroy(id);
            musicDefaultRepository.destroy(id);
        }catch(Exception e){
            log.error("houseId{},message:[{}]",id,e.getMessage());
        }
    }

    public void destroy(){
        musicDefaultRepository.destroy("");
        Iterator<House> iterator = houses.iterator();
        while(iterator.hasNext()){
            House house = iterator.next();
            if(!JusicProperties.HOUSE_DEFAULT_ID.equals(house.getId()) && (house.getEnableStatus() == null || !house.getEnableStatus())){
                sessionRepository.destroy(house.getId());
                configRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
                musicPickRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
                musicBlackRepository.destroy(house.getId());
                sessionBlackRepository.destroy(house.getId());
                musicDefaultRepository.destroy(house.getId());
                iterator.remove();
            }else{
                sessionRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
//                musicPickRepository.destroy(house.getId());
                sessionBlackRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
            }
        }
        housesRespository.destroy(houses);
    }

    public boolean remove(String id){
        House house = new House();
        house.setId(id);
        return houses.remove(house);
    }

    /**
     * 清理 session
     * 清理 config
     * 清理 default
     * 清理 playing
     * 清理 pick
     */
    public CopyOnWriteArrayList<House> clearSurvive() {
        log.info("清理工作开始");
        CopyOnWriteArrayList<House> housesRedis = (CopyOnWriteArrayList<House>) housesRespository.initialize();
        musicDefaultRepository.destroy("");
        for(House house : housesRedis){
            sessionRepository.destroy(house.getId());
            sessionBlackRepository.destroy(house.getId());
//            configRepository.destroy(house.getId());
            musicPlayingRepository.destroy(house.getId());
//            musicPickRepository.destroy(house.getId());
            musicVoteRepository.destroy(house.getId());
//            musicBlackRepository.destroy(house.getId());
        }
        log.info("清理工作完成");
        return housesRedis;
    }

    public void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        configRepository.initialize(houses.get(0).getId());
        musicDefaultRepository.initialize("");
        this.setHouses(houses);
    }

    public RetainKey getRetainKey(String retainKey){
        return retainKeyRepository.getRetainKey(retainKey);
    }

    public void removeRetainKey(String retainKey){
        retainKeyRepository.removeRetainKey(retainKey);
    }

    public void addRetainKey(RetainKey retainKey){
        retainKeyRepository.addRetainKey(retainKey);
    }

    public void updateRetainKey(RetainKey retainKey){
        retainKeyRepository.updateRetainKey(retainKey);
    }

    public List showRetainKey(){
        return retainKeyRepository.showRetainKey();
    }
}

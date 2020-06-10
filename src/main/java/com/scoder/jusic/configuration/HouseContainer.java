package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<House>();

    public HouseContainer(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository,MusicBlackRepository musicBlackRepository,SessionBlackRepository sessionBlackRepository,JusicProperties jusicProperties) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.musicDefaultRepository = musicDefaultRepository;
        this.musicPlayingRepository = musicPlayingRepository;
        this.musicPickRepository = musicPickRepository;
        this.musicVoteRepository = musicVoteRepository;
        this.musicBlackRepository = musicBlackRepository;
        this.sessionBlackRepository = sessionBlackRepository;
        this.jusicProperties = jusicProperties;
        House house = new House();
        house.setName(JusicProperties.HOUSE_DEFAULT_NAME);
        house.setId(JusicProperties.HOUSE_DEFAULT_ID);
        house.setDesc(JusicProperties.HOUSE_DEFAULT_DESC);
        houses.add(house);
    }

    public int size(){
        return houses.size();
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
    }

    public void destroy(String id){
        try{
            log.info("删除房间"+id);
            this.remove(id);
            jusicProperties.removeSessions(id);
            sessionRepository.destroy(id);
            configRepository.destroy(id);
            musicPlayingRepository.destroy(id);
            musicPickRepository.destroy(id);
            musicVoteRepository.destroy(id);
            musicBlackRepository.destroy(id);
            sessionBlackRepository.destroy(id);

        }catch(Exception e){
            log.error("houseId{},message:[{}]",id,e.getMessage());
        }
    }

    public void destroy(){
        musicDefaultRepository.destroy();
        for(House house : houses){
            sessionRepository.destroy(house.getId());
            configRepository.destroy(house.getId());
            musicPlayingRepository.destroy(house.getId());
            musicPickRepository.destroy(house.getId());
            musicVoteRepository.destroy(house.getId());
            musicBlackRepository.destroy(house.getId());
            sessionBlackRepository.destroy(house.getId());
        }
    }

    public boolean remove(String id){
        House house = new House();
        house.setId(id);
        return houses.remove(house);
    }

}

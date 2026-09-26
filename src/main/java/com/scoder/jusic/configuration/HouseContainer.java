package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.model.RetainKey;
import com.scoder.jusic.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

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
    private final HouseOperationLocks houseOperationLocks;
    private volatile CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<House>();
    private volatile ConcurrentHashMap<String, House> housesById = new ConcurrentHashMap<>();

    public HouseContainer(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository,MusicBlackRepository musicBlackRepository,SessionBlackRepository sessionBlackRepository,JusicProperties jusicProperties,HousesRespository housesRespository,RetainKeyRepository retainKeyRepository, HouseOperationLocks houseOperationLocks) {
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
        this.houseOperationLocks = houseOperationLocks;
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
        CopyOnWriteArrayList<House> checked = houses == null
                ? new CopyOnWriteArrayList<>() : houses;
        ConcurrentHashMap<String, House> nextIndex = new ConcurrentHashMap<>();
        for (House house : checked) {
            if (house != null && house.getId() != null) {
                nextIndex.put(house.getId(), house);
            }
        }
        this.houses = checked;
        this.housesById = nextIndex;
    }

    public boolean isBeyondIpHouse(String ip,int limit){
        int count = 0;
        for(House house : houses){
            if(house != null && java.util.Objects.equals(
                    house.getRemoteAddress(), ip)){
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
        return id == null ? null : housesById.get(id);
    }

    public Boolean contains(String id){
        return id != null && housesById.containsKey(id);
    }

    public void add(House house){
        ReentrantLock lock = houseOperationLocks.getLock(house.getId());
        lock.lock();
        try {
            if (get(house.getId()) != null) {
                return;
            }
            // Make even a config-only partial room discoverable after a JVM
            // crash. If initialization fails, the durable presence index lets
            // the cleanup job retry all room-key families later.
            sessionRepository.trackPresenceHouse(house.getId());
            configRepository.initialize(house.getId());
            houses.add(house);
            housesById.put(house.getId(), house);
            if(house.getEnableStatus() != null && house.getEnableStatus()){
                this.refreshHouses();
            }
        } finally {
            lock.unlock();
        }
    }

    public void refreshHouses(){
        CopyOnWriteArrayList<House> retainHouse = new CopyOnWriteArrayList<>();
        for(House house : houses){
            if(house.getEnableStatus() != null && house.getEnableStatus()){
                retainHouse.add(house);
            }
        }
        housesRespository.destroy(retainHouse);
    }

    public boolean destroy(String id){
        if (id == null) {
            return false;
        }
        ReentrantLock lock = houseOperationLocks.getLock(id);
        lock.lock();
        try{
            if (jusicProperties.hasSessions(id)) {
                return false;
            }
            Long activeSessions = sessionRepository.size(id);
            if (activeSessions != null && activeSessions > 0L) {
                return false;
            }
            return destroyExistingLocked(id);
        } catch (RuntimeException ex) {
            // Redis availability is required to prove that another node does
            // not still own a live connection. Fail closed and keep the room.
            log.warn("Unable to verify that house {} is empty; destruction was rejected: {}",
                    id, ex.getMessage());
            log.debug("House occupancy verification failure", ex);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Destroys a temporary room only after re-reading room metadata, this
     * node's live-session map and the Redis-backed distributed presence while
     * holding the room lifecycle lock.
     *
     * <p>Redis availability is required to prove that another node does not
     * still own a live connection. Any verification failure therefore keeps
     * the room discoverable for a later retry.</p>
     *
     * @return true only when this call completed the room cleanup
     */
    public boolean destroyIfTemporaryAndEmpty(String id) {
        if (id == null) {
            return false;
        }
        ReentrantLock lock = houseOperationLocks.getLock(id);
        lock.lock();
        try {
            House house = get(id);
            if (house == null
                    || JusicProperties.HOUSE_DEFAULT_ID.equals(id)
                    || Boolean.TRUE.equals(house.getEnableStatus())
                    || jusicProperties.hasSessions(id)) {
                return false;
            }
            Long activeSessions = sessionRepository.size(id);
            if (activeSessions != null && activeSessions > 0L) {
                return false;
            }
            return destroyExistingLocked(id);
        } catch (RuntimeException ex) {
            // A failed Redis read is not proof that the room is empty. Keep
            // both the local registry entry and every room key intact.
            log.warn("Unable to verify that temporary house {} is empty; "
                            + "destruction was rejected: {}",
                    id, ex.getMessage());
            log.debug("Temporary house occupancy verification failure", ex);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private boolean destroyExistingLocked(String id) {
        if (get(id) == null) {
            return false;
        }
        try {
            // Keep the room discoverable when cleanup fails so a later check
            // can retry instead of leaving unreachable Redis residue.
            destroyRedisLocked(id);
            return removeLocked(id);
        }catch(Exception e){
            log.error("houseId:{}, cleanup failed: {}", id, e.getMessage(), e);
            return false;
        }
    }

    public void destroyRedis(String id){
        if (id == null) {
            return;
        }
        ReentrantLock lock = houseOperationLocks.getLock(id);
        lock.lock();
        try {
            destroyRedisLocked(id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes Redis residue for a room that is still unknown and has no raw
     * session HASH fields. All checks and deletion share the lifecycle lock so
     * a concurrent local room creation cannot be erased after it wins the lock.
     */
    public boolean destroyRedisIfUnknownAndEmpty(String id) {
        if (id == null || JusicProperties.HOUSE_DEFAULT_ID.equals(id)) {
            return false;
        }
        ReentrantLock lock = houseOperationLocks.getLock(id);
        lock.lock();
        try {
            if (get(id) != null
                    || jusicProperties.hasSessions(id)) {
                return false;
            }
            Long storedSessions = sessionRepository.storedSize(id);
            if (storedSessions == null || storedSessions != 0L) {
                return false;
            }
            destroyRedisLocked(id);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void destroyRedisLocked(String id){
        sessionRepository.destroy(id);
        configRepository.destroy(id);
        musicPlayingRepository.destroy(id);
        musicPickRepository.destroy(id);
        musicVoteRepository.destroy(id);
        musicBlackRepository.destroy(id);
        sessionBlackRepository.destroy(id);
        musicDefaultRepository.destroy(id);
        // Keep the presence room index as a durable retry tombstone until all
        // room keys above have been removed successfully.
        sessionRepository.untrackPresenceHouse(id);
        // Remove local reachability only after every Redis delete completed.
        // If Redis fails, the still-discoverable room can be retried safely.
        jusicProperties.removeSessions(id);
    }

    public void destroy(){
        musicDefaultRepository.destroy("");
        // CopyOnWriteArrayList iterators do not support remove(). Iterate over
        // a stable snapshot and mutate the owning collection explicitly so an
        // opt-in legacy shutdown cleanup cannot stop halfway with
        // UnsupportedOperationException.
        for (House house : new ArrayList<>(houses)) {
            if(!JusicProperties.HOUSE_DEFAULT_ID.equals(house.getId()) && (house.getEnableStatus() == null || !house.getEnableStatus())){
                sessionRepository.destroy(house.getId());
                configRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
                musicPickRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
                musicBlackRepository.destroy(house.getId());
                sessionBlackRepository.destroy(house.getId());
                musicDefaultRepository.destroy(house.getId());
                houses.remove(house);
                housesById.remove(house.getId(), house);
            }else{
                sessionRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
//                musicPickRepository.destroy(house.getId());
//                sessionBlackRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
            }
        }
        housesRespository.destroy(houses);
    }

    public boolean remove(String id){
        if (id == null) {
            return false;
        }
        ReentrantLock lock = houseOperationLocks.getLock(id);
        lock.lock();
        try {
            return removeLocked(id);
        } finally {
            lock.unlock();
        }
    }

    private boolean removeLocked(String id){
        House house = housesById.remove(id);
        if (house == null) {
            return false;
        }
        boolean removed = houses.remove(house);
        if (!removed) {
            housesById.putIfAbsent(id, house);
        }
        return removed;
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
        CopyOnWriteArrayList<House> housesRedis =
                new CopyOnWriteArrayList<>(housesRespository.initialize());
        musicDefaultRepository.destroy("");
        List<String> houseIds = new ArrayList<>();
        for(House house : housesRedis){
            houseIds.add(house.getId());
            sessionRepository.destroy(house.getId());
//            sessionBlackRepository.destroy(house.getId());
//            configRepository.destroy(house.getId());
            musicPlayingRepository.destroy(house.getId());
//            musicPickRepository.destroy(house.getId());
            musicVoteRepository.destroy(house.getId());
//            musicBlackRepository.destroy(house.getId());
        }
        /**
         * 清理不是永久房的redis key
         */
        Set<String> keys = housesRespository.allKeys();
        if(!keys.isEmpty() && !houseIds.isEmpty()){
            for(String key : keys){
                int index;
                if((index=key.lastIndexOf("_")) != -1 && index < key.length()-1){
                    String houseId = key.substring(index+1);
                    if(!houseIds.contains(houseId)){
                        housesRespository.delKey(key);
                    }
                }
            }
        }
        log.info("清理工作完成");
        return housesRedis;
    }

    /**
     * Loads the room registry without deleting sessions, playback, votes, or
     * scanning unrelated Redis keys. This is the normal node startup path.
     */
    public CopyOnWriteArrayList<House> loadSurvive() {
        return new CopyOnWriteArrayList<>(housesRespository.initialize());
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

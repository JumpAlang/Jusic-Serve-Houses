package com.scoder.jusic.configuration;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author H
 */
@Component
@ConfigurationProperties(prefix = "jusic")
@Data
@ToString
public class JusicProperties {

    /**
     * Node-local WebSocket membership index.
     *
     * <p>All membership mutations must go through {@link #register},
     * {@link #move} or {@link #remove}. The per-house maps remain exposed for
     * compatibility with existing broadcast/read code; mutating those maps
     * directly bypasses admission accounting.</p>
     */
    public static class SessionContainer {
        private static final String UNKNOWN_IP = "unknown";

        private final Map<String, ConcurrentHashMap<String, WebSocketSession>> sessionContainer =
                new ConcurrentHashMap<>();
        private final Map<String, Registration> registrations = new HashMap<>();
        private final Map<String, Integer> roomCounts = new HashMap<>();
        private final Map<String, Integer> ipCounts = new HashMap<>();
        private final ReentrantLock membershipLock = new ReentrantLock();

        private volatile ConnectionCapacityProperties capacityProperties;
        private int nodeCount;

        public SessionContainer() {
            this(new ConnectionCapacityProperties());
        }

        public SessionContainer(ConnectionCapacityProperties capacityProperties) {
            setCapacityProperties(capacityProperties);
        }

        private void setCapacityProperties(ConnectionCapacityProperties capacityProperties) {
            this.capacityProperties = Objects.requireNonNull(capacityProperties, "capacityProperties");
        }

        /**
         * Register a new session, or return an idempotent result for the exact
         * same session already in the same house.
         */
        public ConnectionAdmissionResult register(WebSocketSession session, String houseId, String clientIp) {
            WebSocketSession checkedSession = requireSession(session);
            String checkedHouseId = requireText(houseId, "houseId");
            String checkedIp = normalizeIp(clientIp);

            membershipLock.lock();
            try {
                return registerLocked(checkedSession, checkedHouseId, checkedIp);
            } finally {
                membershipLock.unlock();
            }
        }

        /**
         * Atomically change a registered session's house. If the session is no
         * longer registered (for example after a retry following cleanup), this
         * operation behaves like {@link #register}.
         */
        public ConnectionAdmissionResult move(WebSocketSession session, String targetHouseId, String clientIp) {
            WebSocketSession checkedSession = requireSession(session);
            String checkedTargetHouseId = requireText(targetHouseId, "targetHouseId");
            String checkedIp = normalizeIp(clientIp);

            membershipLock.lock();
            try {
                Registration existing = registrations.get(checkedSession.getId());
                if (existing == null) {
                    return registerLocked(checkedSession, checkedTargetHouseId, checkedIp);
                }
                requireSameSession(existing, checkedSession);

                if (existing.houseId.equals(checkedTargetHouseId) && existing.clientIp.equals(checkedIp)) {
                    return result(ConnectionAdmissionResult.Outcome.ALREADY_REGISTERED,
                            checkedSession, existing.houseId, existing.houseId);
                }
                return moveLocked(existing, checkedSession, checkedTargetHouseId, checkedIp);
            } finally {
                membershipLock.unlock();
            }
        }

        /**
         * Identity-safe removal for close/error callbacks. A delayed callback
         * belonging to an old object cannot remove a newer session that happens
         * to reuse the same id.
         */
        public ConnectionAdmissionResult remove(WebSocketSession session) {
            WebSocketSession checkedSession = requireSession(session);

            membershipLock.lock();
            try {
                Registration existing = registrations.get(checkedSession.getId());
                if (existing == null) {
                    return removeLegacySessionLocked(checkedSession);
                }
                if (existing.session != checkedSession) {
                    return result(ConnectionAdmissionResult.Outcome.STALE_SESSION,
                            checkedSession, existing.houseId, existing.houseId);
                }
                return removeLocked(existing);
            } finally {
                membershipLock.unlock();
            }
        }

        /**
         * Remove by id only when it still belongs to the expected house. This is
         * intended for administrative cleanup paths that do not retain the
         * WebSocketSession object.
         */
        public ConnectionAdmissionResult remove(String sessionId, String expectedHouseId) {
            String checkedSessionId = requireText(sessionId, "sessionId");
            String checkedHouseId = requireText(expectedHouseId, "expectedHouseId");

            membershipLock.lock();
            try {
                Registration existing = registrations.get(checkedSessionId);
                if (existing == null) {
                    ConcurrentHashMap<String, WebSocketSession> houseSessions =
                            getHouseSessionIfPresent(checkedHouseId);
                    WebSocketSession legacySession = houseSessions == null
                            ? null : houseSessions.remove(checkedSessionId);
                    evictHouseIfEmpty(checkedHouseId, houseSessions);
                    return result(legacySession == null
                                    ? ConnectionAdmissionResult.Outcome.ALREADY_REMOVED
                                    : ConnectionAdmissionResult.Outcome.REMOVED,
                            legacySession, checkedHouseId, checkedHouseId);
                }
                if (!existing.houseId.equals(checkedHouseId)) {
                    return result(ConnectionAdmissionResult.Outcome.STALE_SESSION,
                            existing.session, existing.houseId, existing.houseId);
                }
                return removeLocked(existing);
            } finally {
                membershipLock.unlock();
            }
        }

        /**
         * Remove all local sessions for a destroyed house and reconcile all
         * counters. Repeating this operation is safe.
         */
        public void removeHouse(String houseId) {
            String checkedHouseId = requireText(houseId, "houseId");

            membershipLock.lock();
            try {
                ConcurrentHashMap<String, WebSocketSession> houseSessions =
                        getHouseSessionIfPresent(checkedHouseId);
                if (houseSessions != null && !houseSessions.isEmpty()) {
                    throw new IllegalStateException(
                            "cannot remove a house with active WebSocket sessions: "
                                    + checkedHouseId);
                }
                for (Registration registration : registrations.values()) {
                    if (registration.houseId.equals(checkedHouseId)) {
                        throw new IllegalStateException(
                                "cannot remove a house with registered WebSocket sessions: "
                                        + checkedHouseId);
                    }
                }
                sessionContainer.remove(checkedHouseId);
            } finally {
                membershipLock.unlock();
            }
        }

        /**
         * Return the stable concurrent map for a house. computeIfAbsent prevents
         * concurrent first joins from publishing two different maps and losing
         * sessions from the discarded map.
         */
        public ConcurrentHashMap<String, WebSocketSession> getHouseSession(String houseId) {
            String checkedHouseId = requireText(houseId, "houseId");
            return sessionContainer.computeIfAbsent(checkedHouseId, key -> new ConcurrentHashMap<>());
        }

        /** Read-only lookup that never recreates an empty map for a removed room. */
        public ConcurrentHashMap<String, WebSocketSession> getHouseSessionIfPresent(String houseId) {
            String checkedHouseId = requireText(houseId, "houseId");
            return sessionContainer.get(checkedHouseId);
        }

        public int getHouseSessionCount(String houseId) {
            ConcurrentHashMap<String, WebSocketSession> houseSessions =
                    getHouseSessionIfPresent(houseId);
            return houseSessions == null ? 0 : houseSessions.size();
        }

        public boolean hasHouseSessions(String houseId) {
            ConcurrentHashMap<String, WebSocketSession> houseSessions =
                    getHouseSessionIfPresent(houseId);
            return houseSessions != null && !houseSessions.isEmpty();
        }

        public WebSocketSession getSessionIfPresent(String houseId, String sessionId) {
            String checkedSessionId = requireText(sessionId, "sessionId");
            ConcurrentHashMap<String, WebSocketSession> houseSessions =
                    getHouseSessionIfPresent(houseId);
            return houseSessions == null ? null : houseSessions.get(checkedSessionId);
        }

        public Map<String, ConcurrentHashMap<String, WebSocketSession>> get() {
            return sessionContainer;
        }

        public int getNodeCount() {
            membershipLock.lock();
            try {
                return nodeCount;
            } finally {
                membershipLock.unlock();
            }
        }

        public int getRoomCount(String houseId) {
            String checkedHouseId = requireText(houseId, "houseId");
            membershipLock.lock();
            try {
                return count(roomCounts, checkedHouseId);
            } finally {
                membershipLock.unlock();
            }
        }

        public int getIpCount(String clientIp) {
            String checkedIp = normalizeIp(clientIp);
            membershipLock.lock();
            try {
                return count(ipCounts, checkedIp);
            } finally {
                membershipLock.unlock();
            }
        }

        private ConnectionAdmissionResult registerLocked(WebSocketSession session,
                                                         String houseId,
                                                         String clientIp) {
            Registration existing = registrations.get(session.getId());
            if (existing != null) {
                requireSameSession(existing, session);
                if (existing.houseId.equals(houseId) && existing.clientIp.equals(clientIp)) {
                    return result(ConnectionAdmissionResult.Outcome.ALREADY_REGISTERED,
                            session, existing.houseId, existing.houseId);
                }
                return moveLocked(existing, session, houseId, clientIp);
            }

            ensureBelowLimit(
                    ConnectionAdmissionException.Reason.NODE_LIMIT,
                    nodeCount,
                    capacityProperties.getNode()
            );
            ensureBelowLimit(
                    ConnectionAdmissionException.Reason.ROOM_LIMIT,
                    count(roomCounts, houseId),
                    capacityProperties.getRoom()
            );
            ensureBelowLimit(
                    ConnectionAdmissionException.Reason.IP_LIMIT,
                    count(ipCounts, clientIp),
                    capacityProperties.getIp()
            );

            ConcurrentHashMap<String, WebSocketSession> houseSessions = getHouseSession(houseId);
            WebSocketSession occupant = houseSessions.get(session.getId());
            if (occupant != null && occupant != session) {
                throw ConnectionAdmissionException.sessionIdConflict(session.getId());
            }

            houseSessions.put(session.getId(), session);
            registrations.put(session.getId(), new Registration(session, houseId, clientIp));
            nodeCount++;
            increment(roomCounts, houseId);
            increment(ipCounts, clientIp);
            return result(ConnectionAdmissionResult.Outcome.REGISTERED, session, null, houseId);
        }

        private ConnectionAdmissionResult moveLocked(Registration existing,
                                                     WebSocketSession session,
                                                     String targetHouseId,
                                                     String clientIp) {
            if (!existing.houseId.equals(targetHouseId)) {
                ensureBelowLimit(
                        ConnectionAdmissionException.Reason.ROOM_LIMIT,
                        count(roomCounts, targetHouseId),
                        capacityProperties.getRoom()
                );
            }
            if (!existing.clientIp.equals(clientIp)) {
                ensureBelowLimit(
                        ConnectionAdmissionException.Reason.IP_LIMIT,
                        count(ipCounts, clientIp),
                        capacityProperties.getIp()
                );
            }

            ConcurrentHashMap<String, WebSocketSession> sourceSessions =
                    getHouseSessionIfPresent(existing.houseId);
            WebSocketSession sourceOccupant = sourceSessions == null
                    ? null : sourceSessions.get(session.getId());
            if (sourceOccupant != null && sourceOccupant != session) {
                throw ConnectionAdmissionException.sessionIdConflict(session.getId());
            }

            ConcurrentHashMap<String, WebSocketSession> targetSessions = getHouseSession(targetHouseId);
            WebSocketSession targetOccupant = targetSessions.get(session.getId());
            if (targetOccupant != null && targetOccupant != session) {
                throw ConnectionAdmissionException.sessionIdConflict(session.getId());
            }

            String previousHouseId = existing.houseId;
            String previousIp = existing.clientIp;
            if (!previousHouseId.equals(targetHouseId)) {
                if (sourceSessions != null) {
                    sourceSessions.remove(session.getId(), session);
                    evictHouseIfEmpty(previousHouseId, sourceSessions);
                }
                targetSessions.put(session.getId(), session);
                decrement(roomCounts, previousHouseId);
                increment(roomCounts, targetHouseId);
            }
            if (!previousIp.equals(clientIp)) {
                decrement(ipCounts, previousIp);
                increment(ipCounts, clientIp);
            }
            existing.houseId = targetHouseId;
            existing.clientIp = clientIp;
            return result(ConnectionAdmissionResult.Outcome.MOVED,
                    session, previousHouseId, targetHouseId);
        }

        private ConnectionAdmissionResult removeLocked(Registration registration) {
            registrations.remove(registration.session.getId());
            ConcurrentHashMap<String, WebSocketSession> houseSessions =
                    getHouseSessionIfPresent(registration.houseId);
            if (houseSessions != null) {
                houseSessions.remove(registration.session.getId(), registration.session);
                evictHouseIfEmpty(registration.houseId, houseSessions);
            }
            nodeCount--;
            decrement(roomCounts, registration.houseId);
            decrement(ipCounts, registration.clientIp);
            return result(ConnectionAdmissionResult.Outcome.REMOVED,
                    registration.session, registration.houseId, registration.houseId);
        }

        /**
         * Transitional compatibility for sessions inserted through the exposed
         * per-house map before callers are migrated to register/move. Removal is
         * still identity-safe and these untracked entries never affect counters.
         */
        private ConnectionAdmissionResult removeLegacySessionLocked(WebSocketSession session) {
            String removedHouseId = null;
            String conflictingHouseId = null;
            for (Map.Entry<String, ConcurrentHashMap<String, WebSocketSession>> entry
                    : sessionContainer.entrySet()) {
                WebSocketSession occupant = entry.getValue().get(session.getId());
                if (occupant == session) {
                    if (entry.getValue().remove(session.getId(), session)) {
                        removedHouseId = entry.getKey();
                        evictHouseIfEmpty(entry.getKey(), entry.getValue());
                    }
                } else if (occupant != null) {
                    conflictingHouseId = entry.getKey();
                }
            }
            if (removedHouseId != null) {
                return result(ConnectionAdmissionResult.Outcome.REMOVED,
                        session, removedHouseId, removedHouseId);
            }
            if (conflictingHouseId != null) {
                return result(ConnectionAdmissionResult.Outcome.STALE_SESSION,
                        session, conflictingHouseId, conflictingHouseId);
            }
            return result(ConnectionAdmissionResult.Outcome.ALREADY_REMOVED,
                    session, null, null);
        }

        /**
         * Drop empty per-house maps after the last local connection leaves.
         * The identity check prevents a delayed cleanup from removing a newer
         * map that was installed for the same house.
         */
        private void evictHouseIfEmpty(String houseId,
                                       ConcurrentHashMap<String, WebSocketSession> houseSessions) {
            if (houseSessions != null && houseSessions.isEmpty()) {
                sessionContainer.remove(houseId, houseSessions);
            }
        }

        private void requireSameSession(Registration existing, WebSocketSession session) {
            if (existing.session != session) {
                throw ConnectionAdmissionException.sessionIdConflict(session.getId());
            }
        }

        private void ensureBelowLimit(ConnectionAdmissionException.Reason reason, int current, int limit) {
            if (current >= limit) {
                throw ConnectionAdmissionException.capacity(reason, limit, current);
            }
        }

        private WebSocketSession requireSession(WebSocketSession session) {
            WebSocketSession checked = Objects.requireNonNull(session, "session");
            requireText(checked.getId(), "session.id");
            return checked;
        }

        private String requireText(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }

        private String normalizeIp(String clientIp) {
            if (clientIp == null || clientIp.trim().isEmpty()) {
                return UNKNOWN_IP;
            }
            return clientIp.trim();
        }

        private int count(Map<String, Integer> counts, String key) {
            Integer value = counts.get(key);
            return value == null ? 0 : value;
        }

        private void increment(Map<String, Integer> counts, String key) {
            counts.put(key, count(counts, key) + 1);
        }

        private void decrement(Map<String, Integer> counts, String key) {
            int next = count(counts, key) - 1;
            if (next <= 0) {
                counts.remove(key);
            } else {
                counts.put(key, next);
            }
        }

        private ConnectionAdmissionResult result(ConnectionAdmissionResult.Outcome outcome,
                                                 WebSocketSession session,
                                                 String previousHouseId,
                                                 String houseId) {
            return new ConnectionAdmissionResult(outcome, session, previousHouseId, houseId);
        }

        private static final class Registration {
            private final WebSocketSession session;
            private String houseId;
            private String clientIp;

            private Registration(WebSocketSession session, String houseId, String clientIp) {
                this.session = session;
                this.houseId = houseId;
                this.clientIp = clientIp;
            }
        }
    }
    /**
     * 自定义容器，用来装载 session
     */
    private final SessionContainer sessions = new SessionContainer();

    @Autowired
    public void configureConnectionCapacity(ConnectionCapacityProperties capacityProperties) {
        sessions.setCapacityProperties(capacityProperties);
    }

    public ConcurrentHashMap<String,WebSocketSession> getSessions(String houseId){
        return sessions.getHouseSession(houseId);
    }
    public int getSessionCount(String houseId){
        return sessions.getHouseSessionCount(houseId);
    }
    public boolean hasSessions(String houseId){
        return sessions.hasHouseSessions(houseId);
    }
    public WebSocketSession getSessionIfPresent(String houseId, String sessionId){
        return sessions.getSessionIfPresent(houseId, sessionId);
    }
    public SessionContainer getSession(){
        return sessions;
    }
    public void removeSessions(String houseId){
        sessions.removeHouse(houseId);
    }
    /**
     * 音乐到期时间，每首音乐的链接都会有一个失效时间
     */
    private Long musicExpireTime = 1200000L;
    /**
     * 重试次数，从音乐服务那里获取音乐失败的重试机会
     */
    private Integer retryCount = 3;
    /**
     * 投票通过率
     */
    private Float voteRate = 0.3F;
    /**
     * yml file 文件路径名，这是一个默认列表文件
     */
    private String defaultMusicFile;
    /**
     * springboot 在启动的时候将会初始化这个列表，从 defaultListFile 文件中逐行读取
     */
    private static final List<String> FALLBACK_DEFAULT_LIST =
            Collections.unmodifiableList(Arrays.asList(
                    "512359558", "316686", "25718007"));
    private static volatile List<String> defaultList = Collections.emptyList();

    public static void setDefaultListByJob(List<String> list){
        if (list != null && !list.isEmpty()) {
            // Publish one immutable snapshot. Readers can never observe the
            // clear/addAll intermediate state while the scheduled refresh runs.
            defaultList = Collections.unmodifiableList(new ArrayList<>(list));
        }
    }
    public static List<String> getDefaultListForRepository(){
        List<String> snapshot = defaultList;
        return snapshot.isEmpty() ? FALLBACK_DEFAULT_LIST : snapshot;
    }
    /**
     * root 密码
     */
    private String roleRootPassword = "654321";
    /**
     * admin 密码
     */
    private String roleAdminPassword = "123456";

    /**
     * 网易热门歌曲url
     */
    private String wyTopUrl = "https://music.163.com/discover/toplist?id=3778678";
    /**
     * json格式歌曲列表（for禁歌）
     */
    private String musicJson = "D:\\JAVA\\IdeaWorkspaces\\Jusic-serve\\src\\main\\resources\\lizhimusic.json";

    private String aiUrl = "https://studio-api.suno.ai/api/trending/metaplaylist";
    /**
     * 音乐服务
     */
    private String musicServeDomain = "http://localhost";
    private String musicServeDomainQq = "http://localhost";
    private String musicServeDomainMg = "http://localhost";
    private String musicServeDomainKwXm = "http://localhost";

    /**
     * mail send from
     */
    private String mailSendFrom = "18759478272@139.com";
    /**
     * mail send to
     */
    private String mailSendTo = "1040927107@qq.com";

    private String serverJUrl = "https://sc.ftqq.com/SCU64668T909ada7955daadfb64d5e7652b93fb135dad06e659369.send";
    private Integer houseSize = 10;
    private Integer ipHouse = 10;
    private Boolean goodModel = true;
    private Boolean randomModel = false;
    private Boolean musicCircleModel = false;
    private String miniId = "";
    private String miniSecrect = "";
    private String wyAccount = "";
    private String wyPassword = "";
    private Integer playlistSize = 3;
    private Integer foreverlistSize = 33;
    private Boolean listCircleModel = false;

    private String qqUin = "1040927107";
    private String qqMusicKey = "";//需要会员,qqmusic_key可以从Cookie中/客户端的请求体中（comm.authst）获取
    private String qqMusicGuid = "114514";
    private String qqCdnAddr = "http://ws.stream.qqmusic.qq.com/";
    private Integer qqRefreshInterval = 86000;
    public static final String HOUSE_DEFAULT_ID = "DEFAULT";
    public static final String HOUSE_DEFAULT_NAME = "一起听歌吧";
    public static final String HOUSE_DEFAULT_DESC = "永远年轻！永远热泪盈眶！";

    /**
     * redis keys
     */
    @Component
    @ConfigurationProperties(prefix = "jusic.redis.keys")
    @Data
    @ToString
    public static class RedisKeys {

        /**
         * 配置
         */
        private String configHash = "jusic_config";
        /**
         * 存放在线用户
         */
        private String sessionHash = "jusic_session";

        /***
         * 留存码
         */
        private String retainKeyHash = "jusic_retain_key";
        /**
         * 黑名单
         */
        private String sessionBlackHash = "jusic_session_black";
        /**
         * 默认播放列表，如果点歌列表为空则会从这里选出一首推到点歌列表的。
         * 这里存放的全部是 keyword，需要处理一下，拿到 Music info 再存到点歌列表
         */
        private String defaultSet = "jusic_default";
        /**
         * 点歌列表，存放 {@link com.scoder.jusic.model.Music} 对象
         */
        private String pickList = "jusic_pick";
        private String houses = "jusic_houses";

        /**
         * 播放列表，存放 {@link com.scoder.jusic.model.Music} 对象
         */
        private String playingList = "jusic_playing";
        /**
         * 音乐黑名单 id
         */
        private String blackSet = "jusic_black";
        /**
         * 投票集合，用来临时投票切换音乐，存放每个 session 的唯一 id
         */
        private String skipSet = "jusic_skip";

        /**
         * root key, config 子键名
         */
        private String redisRoleRoot = "role_root_password";
        /**
         * admin key, config 子键名
         */
        private String redisRoleAdmin = "role_admin_password";
        /**
         * 音乐最后推送时间, config 子键名
         */
        private String lastMusicPushTime = "last_music_push_time";
        /**
         * 正在播放的音乐时长, config 子键名
         */
        private String lastMusicDuration = "last_music_duration";
        /**
         * 音乐推送开关, config 子键名
         */
        private String switchMusicPush = "switch_music_push";

        private String switchMusicEnable ="switch_music_enable";
        private String searchMusicEnable ="search_music_enable";
        private String pickWaitMinutes ="pick_wait_minutes";
        private String clearQueuedOnLeave ="clear_queued_on_leave";
        private String defaultPlaylistRandom = "default_playlist_random";
        private String goodModel = "good_model";
        private String randomModel = "random_model";
        private String musicCircleModel = "music_circle_model";
        private String listCircleModel = "list_circle_model";

        /**
         * 投票通过率, config 子键名
         */
        private String voteSkipRate = "vote_skip_rate";

        private final JusicEnvironment jusicEnvironment;

        public RedisKeys(JusicEnvironment jusicEnvironment) {
            this.jusicEnvironment = jusicEnvironment;
        }

        public String getConfigHash() {
            return this.configHash + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getSessionHash() {
            return this.sessionHash + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getRetainKeyHash() {
            return this.retainKeyHash + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getSessionBlackHash() {
            return this.sessionBlackHash + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getDefaultSet() {
            return this.defaultSet + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getPickList() {
            return this.pickList + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getHouses() {
            return this.houses + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getPlayingList() {
            return this.playingList + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getBlackSet() {
            return blackSet + "_" + jusicEnvironment.getServerPort()+"_";
        }

        public String getSkipSet() {
            return this.skipSet + "_" + jusicEnvironment.getServerPort()+"_";
        }
    }

}

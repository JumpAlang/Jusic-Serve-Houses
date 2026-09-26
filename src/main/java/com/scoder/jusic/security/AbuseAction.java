package com.scoder.jusic.security;

/**
 * Coarse action groups keep rate-limit keys and metrics bounded.
 */
public enum AbuseAction {
    HANDSHAKE,
    MESSAGE,
    CHAT,
    PICK_MUSIC,
    SEARCH,
    VOTE,
    AUTH,
    ADMIN_NOTIFY,
    ROOM_CREATE,
    ROOM_ENTER,
    HOUSE_QUERY,
    MINI_CODE,
    BILI_PROXY,
    ADMIN_API;

    public static boolean isKnownDestination(String destination) {
        if (destination == null) {
            return false;
        }
        switch (destination) {
            case "/chat":
            case "/chat/black":
            case "/chat/announce":
            case "/chat/unblack":
            case "/chat/blackuser":
            case "/chat/picture/search":
            case "/music/pick":
            case "/music/skip/vote":
            case "/music/order":
            case "/music/clear":
            case "/music/clearDefaultPlayList":
            case "/music/delete":
            case "/music/top":
            case "/music/black":
            case "/music/unblack":
            case "/music/setDefaultPlaylist":
            case "/music/blackmusic":
            case "/music/playlistSize":
            case "/music/defaultPlaylist":
            case "/music/search":
            case "/music/searchsonglist":
            case "/music/searchuser":
            case "/mail/send":
            case "/setting/name":
            case "/auth/root":
            case "/auth/admin":
            case "/house/add":
            case "/house/enter":
            case "/house/search":
            case "/house/houseuser":
                return true;
            default:
                return startsWithAny(destination,
                        "/chat/notice/",
                        "/music/good/",
                        "/music/volumn/",
                        "/music/vote/",
                        "/music/banswitch/",
                        "/music/goodmodel/",
                        "/music/musiccirclemodel/",
                        "/music/musiclistmodel/",
                        "/music/randommodel/",
                        "/music/banchoose/",
                        "/auth/adminpwd/",
                        "/auth/rootpwd/",
                        "/auth/setPicker/",
                        "/auth/setNoPicker/",
                        "/auth/setVoter/",
                        "/auth/setNoVoter/",
                        "/house/retain/");
        }
    }

    public static AbuseAction fromDestination(String destination) {
        if (destination == null) {
            return MESSAGE;
        }
        if ("/chat".equals(destination)) {
            return CHAT;
        }
        if (destination.startsWith("/chat/picture/search")
                || destination.startsWith("/music/search")) {
            return SEARCH;
        }
        if (destination.startsWith("/music/pick")) {
            return PICK_MUSIC;
        }
        if (destination.startsWith("/music/skip/vote")
                || destination.startsWith("/music/good/")) {
            return VOTE;
        }
        if (destination.startsWith("/auth/admin")
                || destination.startsWith("/auth/root")) {
            return AUTH;
        }
        if (destination.startsWith("/mail/send")
                || destination.startsWith("/chat/notice/")
                || destination.startsWith("/chat/announce")) {
            return ADMIN_NOTIFY;
        }
        if (destination.startsWith("/house/add")) {
            return ROOM_CREATE;
        }
        if (destination.startsWith("/house/enter")) {
            return ROOM_ENTER;
        }
        if (destination.equals("/music/defaultPlaylist") || destination.startsWith("/house/search")
                || destination.startsWith("/house/houseuser")) {
            return HOUSE_QUERY;
        }
        return MESSAGE;
    }

    public static AbuseAction fromHttpPath(String path) {
        if (path == null) {
            return null;
        }
        if ("/house/add".equals(path)) {
            return ROOM_CREATE;
        }
        if ("/house/enter".equals(path)) {
            return ROOM_ENTER;
        }
        if ("/house/search".equals(path) || "/house/get".equals(path)) {
            return HOUSE_QUERY;
        }
        if ("/house/getMiniCode".equals(path)) {
            return MINI_CODE;
        }
        if (path.startsWith("/bili/")) {
            return BILI_PROXY;
        }
        if (path.startsWith("/netease/") || path.startsWith("/qqmusic/")) {
            return ADMIN_API;
        }
        if (path.startsWith("/house/edit")
                || path.startsWith("/house/setSize/")
                || path.startsWith("/house/addRetainKey/")
                || path.startsWith("/house/showRetainKey")
                || path.startsWith("/house/removeRetainKey/")
                || path.startsWith("/user/get")) {
            return ADMIN_API;
        }
        return null;
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix) && value.length() > prefix.length()) {
                return true;
            }
        }
        return false;
    }
}

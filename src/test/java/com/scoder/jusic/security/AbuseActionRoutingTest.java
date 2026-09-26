package com.scoder.jusic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbuseActionRoutingTest {

    @Test
    void websocketDestinationsRouteToTheirBoundedActionGroups() {
        assertEquals(AbuseAction.CHAT, AbuseAction.fromDestination("/chat"));
        assertEquals(AbuseAction.SEARCH, AbuseAction.fromDestination("/chat/picture/search/cat"));
        assertEquals(AbuseAction.SEARCH, AbuseAction.fromDestination("/music/search/周杰伦"));
        assertEquals(AbuseAction.PICK_MUSIC, AbuseAction.fromDestination("/music/pick"));
        assertEquals(AbuseAction.VOTE, AbuseAction.fromDestination("/music/skip/vote"));
        assertEquals(AbuseAction.VOTE, AbuseAction.fromDestination("/music/good/song-1"));
        assertEquals(AbuseAction.AUTH, AbuseAction.fromDestination("/auth/admin"));
        assertEquals(AbuseAction.AUTH, AbuseAction.fromDestination("/auth/root/check"));
        assertEquals(AbuseAction.ADMIN_NOTIFY, AbuseAction.fromDestination("/mail/send"));
        assertEquals(AbuseAction.ADMIN_NOTIFY, AbuseAction.fromDestination("/chat/notice/all"));
        assertEquals(AbuseAction.ADMIN_NOTIFY, AbuseAction.fromDestination("/chat/announce"));
        assertEquals(AbuseAction.ROOM_CREATE, AbuseAction.fromDestination("/house/add"));
        assertEquals(AbuseAction.ROOM_ENTER, AbuseAction.fromDestination("/house/enter"));
        assertEquals(AbuseAction.HOUSE_QUERY, AbuseAction.fromDestination("/house/search"));
        assertEquals(AbuseAction.HOUSE_QUERY, AbuseAction.fromDestination("/house/houseuser"));
    }

    @Test
    void unknownOrMissingWebsocketDestinationUsesGeneralMessagePolicy() {
        assertEquals(AbuseAction.MESSAGE, AbuseAction.fromDestination(null));
        assertEquals(AbuseAction.MESSAGE, AbuseAction.fromDestination("/unclassified/action"));
        assertEquals(AbuseAction.MESSAGE, AbuseAction.fromDestination("/chat/other"));
        assertFalse(AbuseAction.isKnownDestination(null));
        assertFalse(AbuseAction.isKnownDestination("/unclassified/action"));
        assertFalse(AbuseAction.isKnownDestination("/auth/rootpwd/"));
        assertTrue(AbuseAction.isKnownDestination("/chat"));
        assertTrue(AbuseAction.isKnownDestination("/auth/rootpwd/secret"));
    }

    @Test
    void httpPathsRouteOnlyProtectedEndpoints() {
        assertEquals(AbuseAction.ROOM_CREATE, AbuseAction.fromHttpPath("/house/add"));
        assertEquals(AbuseAction.ROOM_ENTER, AbuseAction.fromHttpPath("/house/enter"));
        assertEquals(AbuseAction.HOUSE_QUERY, AbuseAction.fromHttpPath("/house/search"));
        assertEquals(AbuseAction.HOUSE_QUERY, AbuseAction.fromHttpPath("/house/get"));
        assertEquals(AbuseAction.MINI_CODE, AbuseAction.fromHttpPath("/house/getMiniCode"));
        assertEquals(AbuseAction.BILI_PROXY, AbuseAction.fromHttpPath("/bili/search"));
        assertEquals(AbuseAction.ADMIN_API, AbuseAction.fromHttpPath("/netease/search"));
        assertEquals(AbuseAction.ADMIN_API, AbuseAction.fromHttpPath("/qqmusic/search"));
        assertEquals(AbuseAction.ADMIN_API, AbuseAction.fromHttpPath("/house/edit"));
        assertEquals(AbuseAction.ADMIN_API, AbuseAction.fromHttpPath("/house/setSize/20"));
        assertEquals(AbuseAction.ADMIN_API, AbuseAction.fromHttpPath("/user/get"));

        assertNull(AbuseAction.fromHttpPath(null));
        assertNull(AbuseAction.fromHttpPath("/health"));
        assertNull(AbuseAction.fromHttpPath("/house/additional"));
    }
}

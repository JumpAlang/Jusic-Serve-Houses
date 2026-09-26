package com.scoder.jusic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    void identifierMatchersKeepTheirExistingBoundaries() {
        assertTrue(StringUtils.isQQMusicId("AbCdEf12345678"));
        assertFalse(StringUtils.isQQMusicId("AbCdEf1234567"));
        assertTrue(StringUtils.isWYMusicId("123456"));
        assertFalse(StringUtils.isWYMusicId("12345"));
        assertTrue(StringUtils.isGDMusicId("*123"));
        assertTrue(StringUtils.isDTMusicId("*123"));
        assertFalse(StringUtils.isGDMusicId("123"));
        assertTrue(StringUtils.isUserId("123"));
        assertFalse(StringUtils.isUserId("12a"));
        assertTrue(StringUtils.isMGMusicId("AbCd1234567"));
        assertFalse(StringUtils.isMGMusicId("AbCd123456"));
    }

    @Test
    void nullIdentifierAndContentInputsNeverMatch() {
        assertFalse(StringUtils.isQQMusicId(null));
        assertFalse(StringUtils.isWYMusicId(null));
        assertFalse(StringUtils.isGDMusicId(null));
        assertFalse(StringUtils.isDTMusicId(null));
        assertFalse(StringUtils.isUserId(null));
        assertFalse(StringUtils.isMGMusicId(null));
        assertFalse(StringUtils.isPlayListIds(null));
        assertFalse(StringUtils.isSessionId(null));
        assertFalse(StringUtils.isUrlSpecialCharacter(null));
        assertNull(StringUtils.getSessionId(null));
    }

    @Test
    void playlistParsingHandlesLeadingAndSeparatorOnlyInput() {
        assertTrue(StringUtils.isPlayListIds("1, 2，3"));
        assertTrue(StringUtils.isPlayListIds("*legacy-value"));
        assertArrayEquals(new String[]{"1", "2", "3"},
                StringUtils.splitPlayListIds(", 1，2 3"));
        assertArrayEquals(new String[0],
                StringUtils.splitPlayListIds(" ,， \t"));
        assertArrayEquals(new String[0],
                StringUtils.splitPlayListIds(""));
        assertArrayEquals(new String[0],
                StringUtils.splitPlayListIds(null));
    }

    @Test
    void sessionExtractionKeepsUnanchoredLegacyMatching() {
        assertTrue(StringUtils.isSessionId("prefixABCDEFGH!"));
        assertEquals("abc12345",
                StringUtils.getSessionId("hello @abc12345!"));
        assertNull(StringUtils.getSessionId("@short"));
    }

    @Test
    void encodeStringPreservesLegacyReplacementOrder() {
        assertEquals("a%2520b%253Fc%25d%23e%26f%3Dg%2Fh%2Bi",
                StringUtils.encodeString("a b?c%d#e&f=g/h+i"));
        assertEquals("", StringUtils.encodeString(new String("")));
        assertNull(StringUtils.encodeString(null));
    }

    @Test
    void ipAndLengthHelpersHandleNullAndEmptyValues() throws Exception {
        assertEquals("127.0.*.*",
                StringUtils.desensitizeIPV4("127.0.0.1"));
        assertEquals("", StringUtils.desensitizeIPV4(""));
        assertNull(StringUtils.desensitizeIPV4(null));
        assertEquals(3, StringUtils.getLength("A中"));
        assertEquals(0, StringUtils.getLength((String) null));
        assertEquals(0, StringUtils.getLength(
                null, StringUtils.ENCODE_UTF_8));
    }

    @Test
    void lyricsDurationRejectsMissingOrMalformedTrailingTags() {
        assertEquals(3_723_000L,
                StringUtils.strToMillisSecond("01:02:03"));
        assertEquals(123_045L,
                StringUtils.lyricStrToMillisSecond("02:03.45"));
        assertEquals(123_045L,
                StringUtils.getLyricsDuration(
                        "[00:01.20]\n[02:03.45]"));
        assertNull(StringUtils.getLyricsDuration(null));
        assertNull(StringUtils.getLyricsDuration(""));
        assertNull(StringUtils.getLyricsDuration("plain lyrics"));
        assertNull(StringUtils.getLyricsDuration("[ar:artist]"));
        assertNull(StringUtils.getLyricsDuration("[00:01.20"));
    }

    @Test
    void urlSpecialCharacterDetectionKeepsItsExistingSet() {
        assertTrue(StringUtils.isUrlSpecialCharacter("a+b"));
        assertTrue(StringUtils.isUrlSpecialCharacter("a b"));
        assertFalse(StringUtils.isUrlSpecialCharacter("abc_def-123"));
        assertFalse(StringUtils.isUrlSpecialCharacter(""));
    }
}

package com.scoder.jusic.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String utils
 *
 * @author H
 */
public class StringUtils {

    public static final String ENCODE_GBK = "GBK";
    public static final String ENCODE_UTF_8 = "UTF-8";

    private static final Pattern IPV4_SEPARATOR_PATTERN =
            Pattern.compile("\\.");
    private static final Pattern QQ_MUSIC_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{14}$");
    private static final Pattern WY_MUSIC_ID_PATTERN =
            Pattern.compile("^[0-9]{6,}$");
    private static final Pattern STAR_MUSIC_ID_PATTERN =
            Pattern.compile("^\\*[0-9]+$");
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("^[0-9]+$");
    private static final Pattern MG_MUSIC_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{11}$");
    private static final Pattern PLAY_LIST_IDS_PATTERN =
            Pattern.compile("^[,\\s，]*\\d+([,\\s，]+\\d*[,\\s，]*)*$");
    private static final Pattern PLAY_LIST_SEPARATOR_PATTERN =
            Pattern.compile("[,\\s，]+");
    private static final Pattern SESSION_ID_PATTERN =
            Pattern.compile("[a-zA-Z0-9]{8,}");
    private static final Pattern MENTIONED_SESSION_ID_PATTERN =
            Pattern.compile("@[a-zA-Z0-9]{8,}");
    private static final Pattern URL_SPECIAL_CHARACTER_PATTERN =
            Pattern.compile("[\\s\\?%#&=/\\+]+");

    private static final Pattern ENCODE_WHITESPACE_PATTERN =
            Pattern.compile("\\s+");
    private static final Pattern ENCODE_QUESTION_PATTERN =
            Pattern.compile("\\?");
    private static final Pattern ENCODE_PERCENT_PATTERN =
            Pattern.compile("%");
    private static final Pattern ENCODE_HASH_PATTERN =
            Pattern.compile("#");
    private static final Pattern ENCODE_AMPERSAND_PATTERN =
            Pattern.compile("&");
    private static final Pattern ENCODE_EQUALS_PATTERN =
            Pattern.compile("=");
    private static final Pattern ENCODE_SLASH_PATTERN =
            Pattern.compile("/");
    private static final Pattern ENCODE_PLUS_PATTERN =
            Pattern.compile("\\+");

    /**
     * 根据给定编码方式获取长度
     *
     * @param str    字符串
     * @param encode 给定编码方式
     * @return 长度
     * @throws UnsupportedEncodingException 异常
     */
    public static int getLength(String str, String encode) throws UnsupportedEncodingException {
        return str == null ? 0 : str.getBytes(encode).length;
    }

    public static int getLength(String s) {
        if (s == null) {
            return 0;
        }
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255) {
                length++;
            } else {
                length += 2;
            }
        }
        return length;
    }

    /**
     * ipv4 脱敏处理
     *
     * @param ipv4 待脱敏的 ip
     * @return 127.0.*.*
     */
    public static String desensitizeIPV4(String ipv4) {
        if (ipv4 == null || ipv4.isEmpty()) {
            return ipv4;
        }
        String[] split = IPV4_SEPARATOR_PATTERN.split(ipv4);
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i >= split.length / 2) {
                ip.append("*");
            } else {
                ip.append(split[i]);
            }
            if (i != split.length - 1) {
                ip.append(".");
            }
        }
        return ip.toString();
    }

    public static boolean isQQMusicId(String id) {
        return matches(QQ_MUSIC_ID_PATTERN, id);
    }

    public static boolean isWYMusicId(String id) {
        return matches(WY_MUSIC_ID_PATTERN, id);
    }

    public static boolean isGDMusicId(String id) {
        return matches(STAR_MUSIC_ID_PATTERN, id);
    }

    public static boolean isDTMusicId(String id) {
        return matches(STAR_MUSIC_ID_PATTERN, id);
    }

    public static boolean isUserId(String id) {
        return matches(USER_ID_PATTERN, id);
    }

    public static boolean isMGMusicId(String id) {
        return matches(MG_MUSIC_ID_PATTERN, id);
    }

    public static boolean isPlayListIds(String id) {
        return matches(PLAY_LIST_IDS_PATTERN, id)
                || (id != null && id.startsWith("*"));
    }

    public static String[] splitPlayListIds(String id) {
        if (id == null || id.isEmpty()) {
            return new String[0];
        }
        String[] ids = PLAY_LIST_SEPARATOR_PATTERN.split(id);
        if (ids.length == 0) {
            return ids;
        }
        if ("".equals(ids[0])) {
            String[] newIds = new String[ids.length - 1];
            System.arraycopy(ids, 1, newIds, 0, ids.length - 1);
            return newIds;
        } else {
            return ids;
        }
    }

    public static boolean isSessionId(String id) {
        return finds(SESSION_ID_PATTERN, id);
    }

    public static String getSessionId(String id) {
        if (id == null) {
            return null;
        }
        Matcher m = MENTIONED_SESSION_ID_PATTERN.matcher(id);
        if (m.find()) {
            return m.group().substring(1);
        } else {
            return null;
        }
    }

    public static Long getLyricsDuration(String lyrics) {
        if (lyrics != null && !lyrics.isEmpty()) {
            int startPos = lyrics.lastIndexOf("[");
            int endPos = lyrics.lastIndexOf("]");
            if (startPos < 0 || endPos <= startPos) {
                return null;
            }
            String durationStr = lyrics.substring(startPos + 1, endPos);
            try {
                return lyricStrToMillisSecond(durationStr);
            } catch (RuntimeException invalidDuration) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static long strToMillisSecond(String durationStr) {
        int hour = Integer.valueOf(durationStr.substring(0, 2));
        int minutes = Integer.valueOf(durationStr.substring(3, 5));
        int second = Integer.valueOf(durationStr.substring(6, 8));
        long sumMillisecond = hour * 60 * 60 * 1000 + minutes * 60 * 1000 + second * 1000;
        return sumMillisecond;
    }

    public static long lyricStrToMillisSecond(String durationStr) {
        int minutes = Integer.valueOf(durationStr.substring(0, 2));
        int second = Integer.valueOf(durationStr.substring(3, 5));
        int millisecond = Integer.valueOf(durationStr.substring(6, 8));
        long sumMillisecond = minutes * 60 * 1000 + second * 1000 + millisecond;
        return sumMillisecond;
    }

    public static String encodeString(String param) {
        if (param == null || param.isEmpty()) {
            return param;
        }
        String encoded = ENCODE_WHITESPACE_PATTERN.matcher(param)
                .replaceAll("%20");
        encoded = ENCODE_QUESTION_PATTERN.matcher(encoded)
                .replaceAll("%3F");
        encoded = ENCODE_PERCENT_PATTERN.matcher(encoded)
                .replaceAll("%25");
        encoded = ENCODE_HASH_PATTERN.matcher(encoded)
                .replaceAll("%23");
        encoded = ENCODE_AMPERSAND_PATTERN.matcher(encoded)
                .replaceAll("%26");
        encoded = ENCODE_EQUALS_PATTERN.matcher(encoded)
                .replaceAll("%3D");
        encoded = ENCODE_SLASH_PATTERN.matcher(encoded)
                .replaceAll("%2F");
        return ENCODE_PLUS_PATTERN.matcher(encoded).replaceAll("%2B");
    }

    public static boolean isUrlSpecialCharacter(String str) {
        return finds(URL_SPECIAL_CHARACTER_PATTERN, str);
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }

    private static boolean finds(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).find();
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        String a = "dsdfsfs夺@njhiivhg，@3jhiivhg非机动车顶起顶起";
        System.out.println(getSessionId(a));
        String b = "33";
        System.out.println(isGDMusicId(b));

        String c = ",2,   333d333   ,3232,232 322 ，233，223， ,323,";
        String d = "24381616";
        System.out.println(isPlayListIds(c));
        System.out.println(isPlayListIds(d));
        System.out.println(Arrays.toString(splitPlayListIds(c)));
        System.out.println(Arrays.toString(splitPlayListIds(d)));
        String e = "asdfs%df_d";
        System.out.println(StringUtils.isUrlSpecialCharacter(e));
    }

}

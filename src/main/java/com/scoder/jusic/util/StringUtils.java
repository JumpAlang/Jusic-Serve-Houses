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

    /**
     * 根据给定编码方式获取长度
     *
     * @param str    字符串
     * @param encode 给定编码方式
     * @return 长度
     * @throws UnsupportedEncodingException 异常
     */
    public static int getLength(String str, String encode) throws UnsupportedEncodingException {
        return str.getBytes(encode).length;
    }

    public static int getLength(String s) {
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
        String[] split = ipv4.split("\\.");
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

    public static boolean isQQMusicId(String id){
        String regEx="^[a-zA-Z0-9]{14}$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }

    public static boolean isWYMusicId(String id){
        String regEx="^[0-9]{6,}$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }
    public static boolean isGDMusicId(String id){
        String regEx="^\\*[0-9]+$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }
    public static boolean isDTMusicId(String id){
        String regEx="^\\*[0-9]+$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }
    public static boolean isUserId(String id){
        String regEx="^[0-9]+$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }

    public static boolean isMGMusicId(String id){
        String regEx="^[a-zA-Z0-9]{11}$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }

    public static boolean isPlayListIds(String id){
        String regEx="^[,\\s，]*\\d+([,\\s，]+\\d*[,\\s，]*)*$";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result || id.startsWith("*");
    }

    public static String[] splitPlayListIds(String id){
        String regEx="[,\\s，]+";
        String[] ids = id.split(regEx);
        if("".equals(ids[0])){
            String[] newIds = new String[ids.length-1];
            System.arraycopy(ids,1,newIds,0,ids.length-1);
            return newIds;
        }else{
            return ids;
        }
    }

    public static boolean isSessionId(String id){
        String regEx="[a-zA-Z0-9]{8,}";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        boolean result=m.find();
        return result;
    }

    public static String getSessionId(String id){
        String regEx="@[a-zA-Z0-9]{8,}";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(id);
        if (m.find()) {
            return m.group().substring(1);
        }else{
            return null;
        }
    }
    public static Long getLyricsDuration(String lyrics){
        if(lyrics != null && !"".equals(lyrics)){
            int startPos = lyrics.lastIndexOf("[");
            int endPos = lyrics.lastIndexOf("]");
            String durationStr = lyrics.substring(startPos+1,endPos);
            return lyricStrToMillisSecond(durationStr);
        }else {
            return null;
        }
    }

    public static long strToMillisSecond(String durationStr){
        int hour = Integer.valueOf(durationStr.substring(0,2));
        int minutes = Integer.valueOf(durationStr.substring(3,5));
        int second = Integer.valueOf(durationStr.substring(6,8));
        long sumMillisecond = hour*60*60*1000+minutes*60*1000+second*1000;
        return sumMillisecond;
    }
    public static long lyricStrToMillisSecond(String durationStr){
        int minutes = Integer.valueOf(durationStr.substring(0,2));
        int second = Integer.valueOf(durationStr.substring(3,5));
        int millisecond = Integer.valueOf(durationStr.substring(6,8));
        long sumMillisecond = minutes*60*1000+second*1000+millisecond;
        return sumMillisecond;
    }

    public static String encodeString(String param){
        if(param != null && param != ""){
            return param.replaceAll("\\s+","%20").replaceAll("\\?","%3F").replaceAll("%","%25").
                    replaceAll("#","%23").replaceAll("&","%26").replaceAll("=","%3D").
                    replaceAll("/","%2F").replaceAll("\\+","%2B");
        }else{
            return param;
        }
    }

    public static boolean isUrlSpecialCharacter(String str){
        String regEx="[\\s\\?%#&=/\\+]+";
        Pattern p= Pattern.compile(regEx);
        Matcher m=p.matcher(str);
        boolean result=m.find();
        return result;
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

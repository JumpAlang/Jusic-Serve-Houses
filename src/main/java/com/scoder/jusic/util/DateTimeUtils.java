package com.scoder.jusic.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author H
 */
public class DateTimeUtils {

    /**
     * 把时间格式化成如：2002-08-03 08:26:30.400 格式的字符串
     */
    public final static String FMT_yyyyMMddHHmmssS = "yyyy-MM-dd HH:mm:ss.S";
    /**
     * 把时间格式化成如：2002-08-03 08:26:16 格式的字符串
     */
    public final static String FMT_yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";
    /**
     * 把时间格式化成如：2002-08-03 08:26 格式的字符串
     */
    public final static String FMT_yyyyMMddHHmm = "yyyy-MM-dd HH:mm";
    /**
     * 常用的格式化时间的格式组，用于本类中格式化字符串成时间型
     */
    private final static String[] formatStr = {FMT_yyyyMMddHHmmssS, FMT_yyyyMMddHHmmss, FMT_yyyyMMddHHmm};


    /**
     * 根据给出的Date值和格式串采用操作系统的默认所在的国家风格来格式化时间，并返回相应的字符串
     *
     * @param date      日期对象
     * @param formatStr 日期格式
     * @return 如果为 null，返回字符串 ""
     */
    public static String formatDateTimeToString(Date date, String formatStr) {
        String reStr = "";
        if (date == null || formatStr == null || formatStr.trim().length() < 1) {
            return reStr;
        }
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern(formatStr);
        reStr = sdf.format(date);
        return reStr == null ? "" : reStr;
    }

    /**
     * 根据给出的Date值字符串和格式串采用操作系统的默认所在的国家风格来格式化时间，并返回相应的字符串
     *
     * @param dateStr   日期字符串
     * @param formatStr 日期格式
     * @return 如果为null，返回""
     * @throws Exception 可能抛出的异常
     */
    public static String formatDateTimeToString(String dateStr, String formatStr) throws Exception {
        String dStr = "";
        if (dateStr != null && dateStr.trim().length() > 0 && formatStr != null && formatStr.trim().length() > 0) {
            dStr = formatDateTimeToString(parseToDate(dateStr), formatStr);
        }
        return dStr;
    }

    /**
     * 将给定的日期时间字符串按操作系统默认的国家风格格式化成"yyyy-MM-dd HH:mm:ss"格式的日期时间串
     *
     * @param dateTimeStr 日期字符串
     * @return 如果为null，返回""
     * @throws Exception 可能抛出的异常
     */
    public static String formatDateTimeToString(String dateTimeStr) throws Exception {
        return formatDateTimeToString(dateTimeStr, FMT_yyyyMMddHHmmss);
    }

    /**
     * 将给定的日期时间按操作系统默认的国家内格格式化成"yyyy-MM-dd HH:mm:ss"格式的日期时间串
     *
     * @param dateTime 日期对象
     * @return 如果为null，返回""
     */
    public static String formatDateTimeToString(Date dateTime) {
        return formatDateTimeToString(dateTime, FMT_yyyyMMddHHmmss);
    }

    /**
     * 按操作系统默认国家的风格把给定的日期字符串格式化为一个Date型日期
     *
     * @param dateTimeStr 日期字符串
     * @return java.util.Date类型对象
     * @throws Exception 可能抛出的异常
     */
    public static Date parseToDate(String dateTimeStr) throws Exception {
        if (dateTimeStr == null || dateTimeStr.trim().length() < 1) {
            throw new IllegalArgumentException("参数dateTimeSt不能是null或空格串！");
        }
        int formatStrLength = formatStr.length;
        int i = 0;
        for (i = 0; i < formatStrLength; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat(formatStr[i]);
            try {
                return sdf.parse(dateTimeStr);
            } catch (ParseException e) {
            }
        }
        throw new Exception("日期格式不正确！");
    }

}

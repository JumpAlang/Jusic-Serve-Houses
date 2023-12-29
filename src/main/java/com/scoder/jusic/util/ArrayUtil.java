package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2023-10-14 11:06
 */

import java.util.Random;

/**
 * @Author Doge
 * @Description
 * @Date 2020/12/15
 */
public class ArrayUtil {
    /**
     * 判断数据是否在数组
     *
     * @param chars
     * @param ch
     * @return
     */
    public static boolean in(char[] chars, char ch) {
        return cn.hutool.core.util.ArrayUtil.indexOf(chars, ch) > -1;
    }

    /**
     * 返回元素在数组中的位置
     *
     * @param array
     * @param val
     * @param <T>
     * @return
     */
    public static <T> int indexOf(T[] array, T val) {
        return cn.hutool.core.util.ArrayUtil.indexOf(array, val);
    }

    /**
     * 原地反转数组，返回原数组
     *
     * @param longs
     */
    public static long[] reverse(long[] longs) {
        return cn.hutool.core.util.ArrayUtil.reverse(longs);
    }

    /**
     * 原地反转数组，返回原数组
     *
     * @param bytes
     */
    public static byte[] reverse(byte[] bytes) {
        return cn.hutool.core.util.ArrayUtil.reverse(bytes);
    }

    /**
     * 随机选取数组中一个元素
     *
     * @param array
     */
    public static <T> T randomChoose(T[] array) {
        Random rand = new Random();
        int num = rand.nextInt(array.length);
        return array[num];
    }

    /**
     * 随机生成指定位 bytes
     *
     * @param n
     * @return
     */
    public static byte[] randomBytes(int n) {
        byte[] bytes = new byte[n];
        Random random = new Random();
        for (int i = 0; i < n; i++) bytes[i] = (byte) random.nextInt(128);
        return bytes;
    }

    /**
     * 连接多个数组
     *
     * @param arrays
     * @param <T>
     * @return
     */
    public static <T> T[] concat(T[]... arrays) {
        return cn.hutool.core.util.ArrayUtil.addAll(arrays);
    }
}
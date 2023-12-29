package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2023-10-14 11:06
 */
import cn.hutool.core.util.ReUtil;

import java.util.List;

/**
 * @Author Doge
 * @Description 正则表达式工具类
 * @Date 2020/12/15
 */
public class RegexUtil {
    /**
     * 正则匹配，返回组 1
     *
     * @param regex
     * @param content
     * @return
     */
    public static String getGroup1(String regex, CharSequence content) {
        return ReUtil.getGroup1(regex, content);
    }

    /**
     * 匹配全部，返回 0 组字符串列表
     *
     * @param regex
     * @param content
     * @return
     */
    public static List<String> findAllGroup0(String regex, CharSequence content) {
        return ReUtil.findAllGroup0(regex, content);
    }

    /**
     * 匹配全部，返回 1 组字符串列表
     *
     * @param regex
     * @param content
     * @return
     */
    public static List<String> findAllGroup1(String regex, CharSequence content) {
        return ReUtil.findAllGroup1(regex, content);
    }

    /**
     * 测试字符串是否完全匹配模式
     *
     * @param regex
     * @param content
     * @return
     */
    public static boolean test(String regex, CharSequence content) {
        return ReUtil.isMatch(regex, content);
    }
}
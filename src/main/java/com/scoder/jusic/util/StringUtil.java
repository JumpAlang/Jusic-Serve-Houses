package com.scoder.jusic.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author JumpAlang
 * @create 2023-07-18 22:20
 */
public class StringUtil {
    /**
     * MD5加密类
     *
     * @param str
     *            要加密的字符串
     * @return 加密后的字符串
     */
    public static String toMD5(String str) {
        try {
            str = org.apache.commons.lang3.StringUtils.defaultString(str);
            if (str.length() == 0) {
                return str;
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] byteDigest = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < byteDigest.length; offset++) {
                i = byteDigest[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            // 32位加密
            return buf.toString();
            // 16位的加密
            // return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static void main(String[] args) {
        System.out.println(StringUtil.toMD5("admin"));
    }
}
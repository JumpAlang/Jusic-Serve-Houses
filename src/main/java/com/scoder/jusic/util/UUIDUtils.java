package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2020-06-10 12:42
 */

public class UUIDUtils {
    //字符库
    public static String[] chars = new String[]{"a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z"};

    public static String getUUID8Len(String uuid) {
        //调用Java提供的生成随机字符串的对象：32位，十六进制，中间包含-
        uuid = uuid.replace("-", "");//UUID.randomUUID().toString().replace("-", "");
        StringBuffer shortBuffer = new StringBuffer();
        for (int i = 0; i < 8; i++) { //分为8组
            String str = uuid.substring(i * 4, i * 4 + 4); //每组4位
            int x = Integer.parseInt(str, 16); //输出str在16进制下的表示
            shortBuffer.append(chars[x % 0x3E]); //用该16进制数取模62（十六进制表示为314（14即E）），结果作为索引取出字符
        }
        return shortBuffer.toString();//生成8位字符
    }

    public static void main(String[] args) {
        System.out.println(getUUID8Len("88081313-9074-7b5c-068c-c6a813ce3601"));
    }
}
package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2023-11-12 15:17
 */


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class EncryptionUtils {

    private static final String MODULUS =
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7"
                    + "b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280"
                    + "104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932"
                    + "575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b"
                    + "3ece0462db0a22b8e7";
    private static final String PUBKEY = "010001";
    private static final byte[] NONCE = "0CoJUm6Qyw8W8jud".getBytes();
    private static final byte[] LINUXKEY = "rFgB&h#%2?^eDg:Q".getBytes();
    private static final String EAPIKEY = "e82ckenh8dichen8";

    public static String MD5(String value) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(value.getBytes());
        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
//
//    public static String weEncrypt(String text) throws Exception {
//        String data = text;
//        byte[] secret = createKey(16);
//        byte[] params = aes(aes(data.getBytes("UTF-8"), NONCE, true, true), secret, true, true);
//        String encSecKey = rsa(secret, PUBKEY, MODULUS);
//        return "{\"params\":\"" + new String(Base64.getEncoder().encode(params)) +
//                "\",\"encSecKey\":\"" + encSecKey + "\"}";
//    }
//
//    public static String linuxEncrypt(String text) throws Exception {
//        byte[] data = aes(text.getBytes(), LINUXKEY, false, true);
//        return "{\"eparams\":\"" + new String(Base64.getEncoder().encode(data)) + "\"}";
//    }

    public static String eapiEncrypt(String url, String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        text = String.valueOf(text);
        String digest = MD5("nobody" + url + "use" + text + "md5forencrypt");
        String data = url + "-36cd479b6b5-" + text + "-36cd479b6b5-" + digest;
        return "{\"params\":\"" + aesEncrypt(data, EAPIKEY.getBytes("UTF-8")) + "\"}";
    }

    public static String aesEncrypt(String text, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(addPadding(text.getBytes("UTF-8")));
            return bytesToHex(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    public static byte[] addPadding(byte[] text) {
        int pad = 16 - text.length % 16;
        byte[] paddedText = Arrays.copyOf(text, text.length + pad);
        Arrays.fill(paddedText, text.length, paddedText.length, (byte) pad);
        return paddedText;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    private static String rsa(byte[] text, String pubkey, String modulus) {
        StringBuilder reversedText = new StringBuilder(new String(text)).reverse();
        long rs = modPow(Long.parseLong(hexlify(reversedText.toString()), 16),
                Long.parseLong(pubkey, 16), Long.parseLong(modulus, 16));
        return String.format("%256s", Long.toHexString(rs)).replace(' ', '0');
    }

    private static byte[] createKey(int size) {
        byte[] key = new byte[size];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private static long modPow(long base, long exponent, long modulus) {
        long result = 1;
        while (exponent > 0) {
            if (exponent % 2 == 1) {
                result = (result * base) % modulus;
            }
            base = (base * base) % modulus;
            exponent /= 2;
        }
        return result;
    }

    private static String hexlify(String value) {
        StringBuilder hexString = new StringBuilder();
        for (char c : value.toCharArray()) {
            hexString.append(Integer.toHexString(c));
        }
        return hexString.toString();
    }
}
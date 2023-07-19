package com.scoder.jusic.util;


import cn.hutool.core.codec.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


/**
 * 加密实用类
 * @author pccc
 */
public final class CryptoUtil2 {
    public static final String ALGORITHM_3DES = "3DES";
    public static final String ALGORITHM_AES = "AES";
    public static final String ALGORITHM_BASE64 = "BASE64";
    public static final String ALGORITHM_DES = "DES";
    public static final String ALGORITHM_DESede = "DESede";
    public static final String ALGORITHM_MD5 = "MD5";
    public static final String ALGORITHM_SHA1 = "SHA-1";

    private static final int HEADLEN = 12;
    private static final int KEYLEN = 24;


    public static void main(String[] args) throws NoSuchAlgorithmException {

//		String s = "123123";
//		System.out.println(s.getBytes().length+" "+byte2hex(s.getBytes()));
//
//		System.out.println(md5("fsfsdfsdf"));
//
//		System.out.println(base64Encode("fsfsdfsdf".getBytes()) + "  " + new String (base64Decode(base64Encode("fsfsdfsdf".getBytes()))));



    }

    public static byte[] getKey(String algorithm)
            throws NoSuchAlgorithmException {
        algorithm = assertAlgorithm(algorithm);
        KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
        keygen.init(getKeySize(algorithm), new SecureRandom());
        SecretKey deskey = keygen.generateKey();
        return deskey.getEncoded();
    }

    public static byte[] encrypt(byte[] input, byte[] key, String algorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        algorithm = assertAlgorithm(algorithm);
        SecretKey secretKey = new SecretKeySpec(key, algorithm);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(1, secretKey);
        return cipher.doFinal(input);
    }

    public static byte[] decrypt(byte[] input, byte[] key, String algorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        algorithm = assertAlgorithm(algorithm);
        SecretKey deskey = new SecretKeySpec(key, algorithm);
        Cipher c1 = Cipher.getInstance(algorithm);
        c1.init(2, deskey);
        byte[] clearByte = c1.doFinal(input);
        return clearByte;
    }

    public static String md5(String plain) throws NoSuchAlgorithmException {
        return byte2hex(md5(plain.getBytes()));
    }

    public static byte[] md5(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM_MD5);
        messageDigest.update(input);
        return messageDigest.digest();
    }

    public static String sha1(String plain) throws NoSuchAlgorithmException {
        byte[] input = plain.getBytes();
        byte[] encrypted = sha1(input);
        return byte2hex(encrypted);
    }

    public static byte[] sha1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(ALGORITHM_SHA1);
        alg.update(input);
        byte[] digest = alg.digest();
        return digest;
    }

    public static String byte2hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length*2);
        String stmp = null;
        String zero = "0";
        for (int i = 0; i < b.length; i++) {
            stmp = Integer.toHexString(b[i] & 0xFF);
            if (stmp.length() == 1) {
                sb.append(zero).append(stmp);
            } else {
                sb.append(stmp);
            }
        }
        return sb.toString().toUpperCase();
    }

    private static String assertAlgorithm(String algorithm) {
        if (ALGORITHM_AES.equalsIgnoreCase(algorithm)) {
            return ALGORITHM_AES;
        }
        return ALGORITHM_DES;
    }

    public static final String encrypt(String plain) {
        if ((plain == null) || (plain.length() == 0))
            return null;
        try {
            byte[] seed = getSeed();
            String seedStr = base64Encode(seed);
            byte[] key = generateKey(seed);
            return seedStr + encrypt(plain, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final String encrypt(String plain, String key)
            throws Exception {
        return encrypt(plain, getKeyByString(key));
    }

    private static final String encrypt(String plain, byte[] key)
            throws Exception {
        byte[] encrypted = encryptByJCE(plain.getBytes(), key);
        return base64Encode(encrypted);
    }

    private static byte[] encryptByJCE(byte[] plainText, byte[] key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        SecretKey securekey = new SecretKeySpec(key, ALGORITHM_DESede);
        Cipher cipher = Cipher.getInstance(ALGORITHM_DESede);
        cipher.init(1, securekey);
        return cipher.doFinal(plainText);
    }

    public static final String decrypt(String cryptograph) {
        if ((cryptograph == null) || (cryptograph.length() == 0))
            return "";
        try {
            String seedStr = cryptograph.substring(0, HEADLEN);
            byte[] seed = base64Decode(seedStr);
            byte[] key = generateKey(seed);
            return decryptByJCE(cryptograph.substring(HEADLEN), key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static final String decrypt(String cryptograph, String key)
            throws Exception {
        return decryptByJCE(cryptograph, getKeyByString(key));
    }

    private static final String decryptByJCE(String cryptograph, byte[] key)
            throws Exception {
        byte[] encrypted = base64Decode(cryptograph);
        return new String(decrypt(encrypted, key));
    }

    private static byte[] decrypt(byte[] cryptograph, byte[] key)
            throws Exception {
        SecretKey securekey = new SecretKeySpec(key, ALGORITHM_DESede);
        Cipher cipher = Cipher.getInstance(ALGORITHM_DESede);
        cipher.init(2, securekey);
        return cipher.doFinal(cryptograph);
    }

    private static byte[] generateKey(byte[] seed) throws Exception {
        byte[] key = { 36, 80, 114, 105, 109, 101, 116, 111, 110, 45, 69, 79,
                83, 32, 87, 105, 108, 108, 95, 87, 105, 110, 33, 36 };

        for (int i = 0; i < seed.length; i++) {
            for (int j = 0; j < key.length; j++) {
                key[j] = (byte) (key[j] ^ seed[i]);
            }
        }
        return key;
    }

    private static byte[] getSeed() {
        long seed = System.currentTimeMillis();
        byte[] seedBytes = String.valueOf(seed).getBytes();
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_MD5);
            return base64Decode(base64Encode(digest.digest(seedBytes))
                    .substring(0, HEADLEN));
        } catch (Exception e) {
        }
        return seedBytes;
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.encode(bytes);
    }

    public static byte[] base64Decode(String str) {
        return Base64.decode(str);
    }

    private static byte[] getKeyByString(String key) {
        byte[] oldKeys = key.getBytes();
        byte[] newKeys = new byte[KEYLEN];
        for (int i = 0; (i < oldKeys.length) && (i != KEYLEN); i++) {
            newKeys[i] = oldKeys[i];
        }
        return newKeys;
    }

    private static int getKeySize(String algorithm) {
        if (algorithm.equals(ALGORITHM_DES))
            return 56;
        if (algorithm.equals("DESede"))
            return 112;
        if (algorithm.equals(ALGORITHM_AES)) {
            return 128;
        }
        return 0;
    }
    /**
     * 适用于mysql与客户端交互时zlib 压缩
     *
     * @param data
     * @return
     */
    public static byte[] compress(byte[] data) {

        byte[] output = null;

        Deflater compresser = new Deflater();
        compresser.setInput(data);
        compresser.finish();

        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        byte[] result = new byte[1024];
        try {
            while (!compresser.finished()) {
                int length = compresser.deflate(result);
                out.write(result, 0, length);
            }
            output = out.toByteArray();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
            compresser.end();
        }

        return output;
    }

    /**
     * 适用于mysql与客户端交互时zlib解压
     *
     * @param data  数据
     * @param off   偏移量
     * @param len   长度
     * @return
     */
    public static byte[] decompress(byte[] data, int off, int len) {

        byte[] output = null;

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data, off, len);

        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            byte[] result = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(result);
                out.write(result, 0, i);
            }
            output = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
            decompresser.end();
        }
        return output;
    }

}
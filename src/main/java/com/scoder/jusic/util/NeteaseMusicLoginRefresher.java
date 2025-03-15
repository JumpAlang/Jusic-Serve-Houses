package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2024-11-18 21:54
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
public class NeteaseMusicLoginRefresher {
    private static final String BASE_URL = "http://interface.music.163.com/eapi/";
    private static final String PATH = "/api/login/token/refresh";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, String> cookieStrToDict(String cookieStr) {
        Map<String, String> cookieDict = new HashMap<>();
        String[] lines = cookieStr.split(";");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    cookieDict.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return cookieDict;
    }

    public static String cookieDictToStr(Map<String, String> cookieDict) {
        StringBuilder cookieStr = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieDict.entrySet()) {
            cookieStr.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        return cookieStr.toString();
    }

    public static void refresh(String cookie) {
        try {

            // 转换 Cookie 字符串为 Map
        Map<String, String> cookieMap = cookieStrToDict(cookie);

        // 构造 Header Map
        Map<String, String> header = new HashMap<>();
        header.put("osver", cookieMap.getOrDefault("osver", "17.4.1"));
        header.put("deviceId", cookieMap.getOrDefault("deviceId", ""));
        header.put("os", cookieMap.getOrDefault("os", "ios"));
        header.put("appver", cookieMap.getOrDefault("appver", "9.0.65"));
        header.put("versioncode", cookieMap.getOrDefault("versioncode", "140"));
        header.put("mobilename", cookieMap.getOrDefault("mobilename", ""));
        header.put("buildver", String.valueOf(System.currentTimeMillis() / 1000));
        header.put("resolution", cookieMap.getOrDefault("resolution", "1920x1080"));
        header.put("__csrf", cookieMap.getOrDefault("__csrf", ""));
        header.put("channel", cookieMap.getOrDefault("channel", ""));
        header.put("requestId", System.currentTimeMillis() + "_" + String.format("%04d", new Random().nextInt(10000)));

        if (cookieMap.containsKey("MUSIC_U")) {
            header.put("MUSIC_U", cookieMap.get("MUSIC_U"));
        }
        if (cookieMap.containsKey("MUSIC_A")) {
            header.put("MUSIC_A", cookieMap.get("MUSIC_A"));
        }
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("header", header);
            requestBodyMap.put("e_r", false);
        // 模拟 eapiEncrypt 加密逻辑
        String encryptedBody = EncryptionUtils.eapiEncrypt(PATH, objectMapper.writeValueAsString(requestBodyMap));

        // 使用 UniRest 发送请求
        HttpResponse<JsonNode> response = Unirest.post(BASE_URL + PATH.substring(5))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0")
                .header("Cookie", cookieDictToStr(header))
                .field("params", encryptedBody)
                .asJson();

        // 检查响应
        if (response.isSuccess()) {
            String responseBody = response.getBody().toString();
            Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
            Object codeObj = body.get("code");
            if (codeObj == null || codeObj.toString() != "200") {
                log.error("网易云刷新登录失败(code: " + codeObj + ")");
            }
        } else {
            log.error("HTTP Request failed with code: " + response.getStatus());
        }
    } catch (Exception e) {
        log.error(e.getMessage(),e);
    }
    }

    public static void main(String[] args) throws Exception {
        refresh("xx");

    }
}


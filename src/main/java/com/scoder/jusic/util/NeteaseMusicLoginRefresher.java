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
            if (codeObj == null || "".equals(codeObj.toString())) {
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
        refresh("WM_TID=HtdbzyMbXztBVVEFQVcqHYHNVzdcaWwa; WEVNSM=1.0.0; ntes_kaola_ad=1; ntes_utid=tid._.ruPXzSIMLoJBBxRVEEeBhDnh4HBoUfug._.0; _ntes_nnid=cb95c1b12d2326334dc65a0f4a84c116,1730987157875; _ntes_nuid=cb95c1b12d2326334dc65a0f4a84c116; __snaker__id=NCS2p7VliQH5XguL; sDeviceId=YD-NVAhQ8SvO2NBExQBREeRfSP2ezrjmcAc; nts_mail_user=a22cloud@163.com:-1:1; NTES_P_UTID=1xALsf8a9R1ruy0As6c9YBjPVT9LBHLa|1730991412; P_INFO=a01lang@163.com|1730991412|0|music|00&99|fuj&1730990843&cloudmusic#fuj&350200#10#0#0|&0|music&cloudmusic|a01lang@163.com; NMTID=00OtHEthFLgrkekrkgansPlHTtcBgYAAAGV2C8Djw; WNMCID=zpofgd.1743088712798.01.0; _iuqxldmzr_=32; WM_NI=gueBk7JPE2cWeoWDaz%2BkD5bIUD7hkPnQqDP9zkLlJe6dhKkeora9NL5rcQeQyY%2F%2BMVtvupJc75yfYP6PboCy28deuG2DaLz9ry2cAFj0r3WQ5bxcrHPyGs0CDdAmNPsUTUs%3D; WM_NIKE=9ca17ae2e6ffcda170e2e6eeb9eb7baeadbe87f167a7ef8ea6d14f838f8ab1d76baa9cac90cf74a88d9aa4e92af0fea7c3b92aa5ababa3ea3d8a908bd3e25da7b199a5e472e9b58cd6e143979cf989ea5cae8fbfa5d333bab5848dc96ff6bca8acea6fadaeafdacd48f7aeadb2e85bbb87e1ccd05eb8aabb91c969f5ebb787dc5cf690ae90c273f2e8ac86f84df29cafadf933b2a7fe84c44eaaf0bed4d65bf7beb884f14494a78994b34988bc8f8ecd5cb18b9cb6f237e2a3; gdxidpyhxdE=IBUC%2BL6ollLR25N7cR4Qh3s2sND5%2FZIJWIs2Acoxd6VS7LPuY%2FgtEweyi4XVvMN8anCKO%2Bwa2bzYcoPghffRCa%2FkI1edVy1hgZb6HB0NUkYuPQKPdZ0uEaCKJ7SxMzpI1q7YpwkyXMjYMimt5PldKrKpMmU2bajfz8TCrRzVLq1mxx3j%3A1749312428303; MUSIC_U=002715B5F6883E819E0974B024AF91F2491969D0C44C1D7A58BE3AB6060C09C98C013AC3BFDC1CC248BB71D3807BB9F7C09C017822B18924C8DE6C50883CA0AEA3FE1525DF3730BED40962605E8BCB589412F532D221221858CC8EB973DAB74B53472DDE6BEA5E2C6D8F235BB18B1384E37CF112FD391269210D32C8D6341A8056E948FFC106DD680E4C519632156463AE31B98AD42F673F6D88199D31FB5BD8A7F16B54DF68ECF6F2216A3BE7B17DB0C620A129C4A566DA1B32FC8A49362FC5A1BA018F4CF2172FB63937D200FF1541182EE72E66A0D93BE620BE94D8BB8B5402C3C0515B3FEDF7E72476E06C074964A8664AFA426EB295A30B7CB6B53FFACF374CB083F27AC56E115CF8B3229A1766F71C787FA5FA921E98ADB1EC8D284A4C5E58167184818C290635C713D1D71AF1AC185D0F156B15AEF82689702FC8E6185B0E0A3812C3A73CAF4E416E2EA7ECE2D8D74393B8720776A888F1E716E39D5D5B; __csrf=3c71ab4103fb0319f2ef4ecf497e9a10; JSESSIONID-WYYY=orzNDXbmlqR9pr%2FHubn%2B%5CTxj0YEJvZ1nArVMkd9xNDZgsdjDV3c0O2ZVSvQeF%2BXCVE2MjBxmD133VJ1MyY2M0goqWpXbhSUWdK%2BOqM6d7H6TTHdGjw87Ut%5C6DJ%2BA%2FUQFuaS4T4atC%2FXgaqcBwxVaR7xzD8%2BPhAwvE3B2Sx57D4phGMgc%3A1749313730937");

    }
}


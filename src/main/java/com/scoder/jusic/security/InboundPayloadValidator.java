package com.scoder.jusic.security;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.configuration.AbuseProtectionProperties;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.Music;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Compatibility-safe validation for the existing flat WebSocket payloads.
 * It rejects invalid input but does not normalize or reshape accepted input.
 */
@Component
public class InboundPayloadValidator {

    private final AbuseProtectionProperties properties;

    public InboundPayloadValidator(AbuseProtectionProperties properties) {
        this.properties = properties;
    }

    public String validateChat(Chat chat) {
        if (chat == null || isBlank(chat.getContent())) {
            return "消息内容不能为空";
        }
        AbuseProtectionProperties.InputLimits limits = properties.getInput();
        if (exceeds(chat.getContent(), limits.getChatCodePoints(), limits.getChatBytes())) {
            return "消息内容过长";
        }
        if (containsDisallowedControlCharacter(chat.getContent())) {
            return "消息内容包含不支持的控制字符";
        }
        return null;
    }

    public String validatePick(Music music) {
        if (music == null) {
            return "点歌参数不能为空";
        }
        if (isBlank(music.getName()) && isBlank(music.getId())) {
            return "歌曲名称或歌曲ID不能为空";
        }
        AbuseProtectionProperties.InputLimits limits = properties.getInput();
        if (exceedsCodePoints(music.getName(), limits.getMusicNameCodePoints())) {
            return "歌曲名称过长";
        }
        if (exceedsCodePoints(music.getId(), limits.getMusicIdCodePoints())) {
            return "歌曲ID过长";
        }
        if (exceedsBytes(music.getName(), limits.getMetadataBytes())
                || exceedsBytes(music.getId(), limits.getMetadataBytes())
                || exceedsBytes(music.getSource(), limits.getMetadataBytes())
                || exceedsBytes(music.getQuality(), limits.getMetadataBytes())) {
            return "点歌参数过长";
        }
        if (containsDisallowedControlCharacter(music.getName())
                || containsDisallowedControlCharacter(music.getId())
                || containsDisallowedControlCharacter(music.getSource())
                || containsDisallowedControlCharacter(music.getQuality())) {
            return "点歌参数包含不支持的控制字符";
        }
        return null;
    }

    public String validateSearch(String keyword, boolean allowBlank, HulkPage<?> page) {
        if (!allowBlank && isBlank(keyword)) {
            return "请输入要搜索的关键字";
        }
        AbuseProtectionProperties.InputLimits limits = properties.getInput();
        if (exceeds(keyword, limits.getSearchCodePoints(), limits.getSearchBytes())) {
            return "搜索关键字过长";
        }
        if (containsDisallowedControlCharacter(keyword)) {
            return "搜索关键字包含不支持的控制字符";
        }
        if (page == null
                || page.getPageIndex() < 1
                || page.getPageIndex() > limits.getMaxPageIndex()
                || page.getPageSize() < 1
                || page.getPageSize() > limits.getMaxPageSize()) {
            return "分页参数不合法";
        }
        return null;
    }

    public String validateMetadata(String... values) {
        int maximum = properties.getInput().getMetadataBytes();
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (exceedsBytes(value, maximum)) {
                return "请求参数过长";
            }
            if (containsDisallowedControlCharacter(value)) {
                return "请求参数包含不支持的控制字符";
            }
        }
        return null;
    }

    private boolean exceeds(String value, int maxCodePoints, int maxBytes) {
        return exceedsCodePoints(value, maxCodePoints) || exceedsBytes(value, maxBytes);
    }

    private boolean exceedsCodePoints(String value, int maximum) {
        return value != null
                && maximum > 0
                && value.codePointCount(0, value.length()) > maximum;
    }

    private boolean exceedsBytes(String value, int maximum) {
        return value != null
                && maximum > 0
                && value.getBytes(StandardCharsets.UTF_8).length > maximum;
    }

    private boolean isBlank(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (!Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint)) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    private boolean containsDisallowedControlCharacter(String value) {
        if (value == null) {
            return false;
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (Character.isISOControl(codePoint)
                    && codePoint != '\n'
                    && codePoint != '\r'
                    && codePoint != '\t') {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }
}

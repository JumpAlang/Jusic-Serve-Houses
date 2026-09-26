package com.scoder.jusic.security;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.configuration.AbuseProtectionProperties;
import com.scoder.jusic.model.Chat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InboundPayloadValidatorTest {

    private AbuseProtectionProperties properties;
    private InboundPayloadValidator validator;

    @BeforeEach
    void setUp() {
        properties = new AbuseProtectionProperties();
        validator = new InboundPayloadValidator(properties);
    }

    @Test
    void chatRejectsMissingOrUnicodeWhitespaceOnlyContent() {
        assertNotNull(validator.validateChat(null));
        assertNotNull(validator.validateChat(chat(null)));
        assertNotNull(validator.validateChat(chat("")));
        assertNotNull(validator.validateChat(chat(" \t\r\n\u3000")));
        assertNull(validator.validateChat(chat("  一起听歌  ")));
    }

    @Test
    void chatCodePointLimitCountsSupplementaryUnicodeAsOneCharacter() {
        properties.getInput().setChatCodePoints(2);
        properties.getInput().setChatBytes(100);

        assertNull(validator.validateChat(chat("😀音")), "two Unicode code points must be accepted");
        assertNotNull(validator.validateChat(chat("😀音乐")), "three Unicode code points must be rejected");
    }

    @Test
    void chatByteLimitUsesUtf8EncodedLengthIndependentlyOfCodePoints() {
        properties.getInput().setChatCodePoints(10);
        properties.getInput().setChatBytes(5);

        assertNull(validator.validateChat(chat("éé")), "two accented characters occupy four UTF-8 bytes");
        assertNotNull(validator.validateChat(chat("ééé")), "six UTF-8 bytes must exceed the five-byte limit");
    }

    @Test
    void chatRejectsUnsafeControlCharactersButAllowsLegacyLineFormatting() {
        assertNull(validator.validateChat(chat("第一行\n第二行\t内容\r")));
        assertNotNull(validator.validateChat(chat("正常\u0000内容")));
        assertNotNull(validator.validateChat(chat("正常\u001b内容")));
    }

    @Test
    void searchPaginationAcceptsInclusiveLimits() {
        AbuseProtectionProperties.InputLimits limits = properties.getInput();

        assertNull(validator.validateSearch("music", false, page(1, 1)));
        assertNull(validator.validateSearch(
                "music",
                false,
                page(limits.getMaxPageIndex(), limits.getMaxPageSize())
        ));
    }

    @Test
    void searchPaginationRejectsNullZeroNegativeAndAboveLimitValues() {
        AbuseProtectionProperties.InputLimits limits = properties.getInput();

        assertNotNull(validator.validateSearch("music", false, null));
        assertNotNull(validator.validateSearch("music", false, page(0, 10)));
        assertNotNull(validator.validateSearch("music", false, page(-1, 10)));
        assertNotNull(validator.validateSearch("music", false, page(limits.getMaxPageIndex() + 1, 10)));
        assertNotNull(validator.validateSearch("music", false, page(1, 0)));
        assertNotNull(validator.validateSearch("music", false, page(1, -1)));
        assertNotNull(validator.validateSearch("music", false, page(1, limits.getMaxPageSize() + 1)));
    }

    @Test
    void searchBlankPolicyAndPayloadLimitsAreEnforcedWithoutChangingAcceptedText() {
        HulkPage<?> page = page(1, 10);
        properties.getInput().setSearchCodePoints(2);
        properties.getInput().setSearchBytes(100);

        assertNotNull(validator.validateSearch(" \t", false, page));
        assertNull(validator.validateSearch(" \t", true, page));
        assertNull(validator.validateSearch("😀音", false, page));
        assertNotNull(validator.validateSearch("😀音乐", false, page));

        properties.getInput().setSearchCodePoints(100);
        properties.getInput().setSearchBytes(5);
        assertNull(validator.validateSearch("éé", false, page));
        assertNotNull(validator.validateSearch("ééé", false, page));
        assertNotNull(validator.validateSearch("正常\u0001", false, page));
    }

    @Test
    void metadataHasIndependentUtf8ByteAndControlCharacterBounds() {
        properties.getInput().setMetadataBytes(5);

        assertNull(validator.validateMetadata("qq", "éé"));
        assertNotNull(validator.validateMetadata("ééé"));
        assertNotNull(validator.validateMetadata("qq\u0000"));
    }

    private static Chat chat(String content) {
        Chat chat = new Chat();
        chat.setContent(content);
        return chat;
    }

    private static HulkPage<?> page(int index, int size) {
        HulkPage<Object> page = new HulkPage<>();
        page.setPageIndex(index);
        page.setPageSize(size);
        return page;
    }
}
